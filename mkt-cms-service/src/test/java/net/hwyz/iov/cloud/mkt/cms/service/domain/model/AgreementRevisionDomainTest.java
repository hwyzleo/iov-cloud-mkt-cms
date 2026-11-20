package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议修订与发布包领域规则单元测试（MKT-CMS-DSN-CR-001 §3.1/§3.2/§5 / AT-002 / AT-003）。
 *
 * @author hwyz_leo
 */
@DisplayName("协议修订与发布包 - 快照摘要/不可变性/成套校验")
class AgreementRevisionDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");

    private static final AgreementScope SCOPE = AgreementScope.of(
            net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene.IVI_ACTIVATION,
            "DEFAULT", "zh-CN", "DEFAULT", "DEFAULT");

    @Test
    @DisplayName("定稿摘要为已保存安全 HTML 精确字节的 SHA-256")
    void 摘要基于精确字节() {
        AgreementDraft draft = draft(AgreementType.PRIVACY_POLICY, "<p>协议正文</p>");

        AgreementRevision revision = AgreementRevision.freezeOf(draft, "REV-TEST-1", "AGR-TEST", "tester", NOW);

        assertEquals(HexSha256.ofUtf8("<p>协议正文</p>"), revision.getContentHash());
        assertEquals("sha256:" + HexSha256.ofUtf8("<p>协议正文</p>"), revision.prefixedContentHash());
        assertEquals(draft.getBody(), revision.getBody());
        assertEquals(draft.getSanitizerVersion(), revision.getSanitizerVersion());
        assertEquals(LocalDate.of(2026, 11, 20), revision.getAgreementDate());
        assertEquals(draft.getLockVersion(), revision.getSourceDraftVersion());
    }

    @Test
    @DisplayName("同一草稿快照重复定稿摘要稳定；正文变化后摘要变化")
    void 摘要稳定性() {
        AgreementDraft draft = draft(AgreementType.PRIVACY_POLICY, "<p>同一份正文</p>");
        AgreementRevision first = AgreementRevision.freezeOf(draft, "REV-TEST-1", "AGR-TEST", "tester", NOW);
        AgreementRevision second = AgreementRevision.freezeOf(draft, "REV-TEST-2", "AGR-TEST", "tester", NOW);

        assertEquals(first.getContentHash(), second.getContentHash());

        AgreementDraft edited = draft.edited(null, null, null,
                SanitizedHtml.builder().body("<p>改后的正文</p>")
                        .sanitizerVersion(AgreementHtmlPolicy.SANITIZER_VERSION).adjusted(true).build(),
                "tester", NOW.plusSeconds(60));
        AgreementRevision afterEdit = AgreementRevision.freezeOf(edited, "REV-TEST-3", "AGR-TEST", "tester", NOW);
        assertNotEquals(first.getContentHash(), afterEdit.getContentHash());
    }

    @Test
    @DisplayName("修订与发布包不提供修改入口（不可变性由类型契约保证）")
    void 修订发布包无修改入口() {
        assertFalse(hasSetter(AgreementRevision.class), "修订不得暴露 setter");
        assertFalse(hasSetter(AgreementBundle.class), "发布包不得暴露 setter");
    }

    @Test
    @DisplayName("发布包必须引用一份隐私政策与一份用户协议")
    void 类型必须成套() {
        AgreementRevision privacy = AgreementRevision.freezeOf(
                draft(AgreementType.PRIVACY_POLICY, "<p>隐私</p>"), "REV-P", "AGR-P", "tester", NOW);
        AgreementRevision user = AgreementRevision.freezeOf(
                draft(AgreementType.USER_AGREEMENT, "<p>用户协议</p>"), "REV-U", "AGR-U", "tester", NOW);

        AgreementException privacyAsUser = assertThrows(AgreementException.class,
                () -> AgreementBundle.publishOf("BDL-1", SCOPE, user, privacy, "req-1", "hash", null,
                        "tester", NOW));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, privacyAsUser.getErrorCode());

        AgreementBundle bundle = AgreementBundle.publishOf("BDL-1", SCOPE, privacy, user, "req-1", "hash",
                null, "tester", NOW);
        assertEquals("REV-P", bundle.getPrivacyRevisionId());
        assertEquals("REV-U", bundle.getUserRevisionId());
        assertTrue(bundle.matchesCurrent(null));
        assertFalse(bundle.matchesCurrent("BDL-2"));
    }

    @Test
    @DisplayName("跨范围修订不能成套发布")
    void 跨范围拒绝() {
        AgreementScope other = AgreementScope.of(
                net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene.IVI_ACTIVATION,
                "CN", "zh-CN", "DEFAULT", "DEFAULT");
        AgreementRevision privacy = AgreementRevision.freezeOf(
                draft(AgreementType.PRIVACY_POLICY, "<p>隐私</p>"), "REV-P", "AGR-P", "tester", NOW);
        AgreementRevision userOtherScope = AgreementRevision.freezeOf(
                draft(AgreementType.USER_AGREEMENT, "<p>用户协议</p>", other), "REV-U", "AGR-U", "tester", NOW);

        AgreementException exception = assertThrows(AgreementException.class,
                () -> AgreementBundle.publishOf("BDL-1", SCOPE, privacy, userOtherScope, "req-1", "hash",
                        null, "tester", NOW));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, exception.getErrorCode());
    }

    @Test
    @DisplayName("发布前提：expectedBundleId 决定是否允许覆盖当前包")
    void 发布前提比较() {
        AgreementRevision privacy = AgreementRevision.freezeOf(
                draft(AgreementType.PRIVACY_POLICY, "<p>隐私</p>"), "REV-P", "AGR-P", "tester", NOW);
        AgreementRevision user = AgreementRevision.freezeOf(
                draft(AgreementType.USER_AGREEMENT, "<p>用户协议</p>"), "REV-U", "AGR-U", "tester", NOW);

        AgreementBundle first = AgreementBundle.publishOf("BDL-1", SCOPE, privacy, user, "req-1", "hash",
                null, "tester", NOW);
        assertFalse(first.matchesCurrent("BDL-EXISTING"));

        AgreementBundle next = AgreementBundle.publishOf("BDL-2", SCOPE, privacy, user, "req-2", "hash",
                "BDL-1", "tester", NOW);
        assertTrue(next.matchesCurrent("BDL-1"));
        assertFalse(next.matchesCurrent(null));
    }

    @Test
    @DisplayName("当前指针切换递增版本且不改写历史指针对象")
    void 指针切换() {
        AgreementCurrent current = AgreementCurrent.empty(SCOPE, NOW);
        AgreementCurrent switched = current.switchTo("BDL-1", NOW.plusSeconds(1));

        assertEquals(0, current.getLockVersion());
        assertEquals(null, current.getBundleId());
        assertEquals(1, switched.getLockVersion());
        assertEquals("BDL-1", switched.getBundleId());
    }

    private AgreementDraft draft(AgreementType type, String body) {
        return draft(type, body, SCOPE);
    }

    private AgreementDraft draft(AgreementType type, String body, AgreementScope scope) {
        return AgreementDraft.create("DRF-TEST", type, scope, "标题", "1.0", LocalDate.of(2026, 11, 20),
                SanitizedHtml.builder().body(body)
                        .sanitizerVersion(AgreementHtmlPolicy.SANITIZER_VERSION).adjusted(false).build(),
                "tester", NOW);
    }

    private boolean hasSetter(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.startsWith("set"));
    }
}
