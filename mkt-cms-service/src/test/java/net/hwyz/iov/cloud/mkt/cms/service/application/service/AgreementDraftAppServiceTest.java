package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.CreateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.FreezeAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.UpdateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementDraftResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementDraftRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementRevisionRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ScopeResolver;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.sanitize.JsoupHtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 协议草稿应用服务单元测试（MKT-CMS-DSN-CR-001 §4 / AT-001 / AT-002）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("协议草稿 - 保存清洗/乐观锁/定稿幂等")
class AgreementDraftAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");
    private static final LocalDate AGREEMENT_DATE = LocalDate.of(2026, 11, 20);

    @Mock
    private AgreementDraftRepository agreementDraftRepository;
    @Mock
    private AgreementRevisionRepository agreementRevisionRepository;
    @Mock
    private AgreementOperationRepository agreementOperationRepository;
    @Mock
    private AgreementTxService agreementTxService;

    private AgreementDraftAppService service;

    @BeforeEach
    void setUp() {
        ScopeResolver resolver = new ScopeResolver(new DefaultOnlyScopeProvider());
        JsoupHtmlSanitizer sanitizer = new JsoupHtmlSanitizer(new AgreementHtmlPolicy(1024 * 1024, 1024 * 1024));
        service = new AgreementDraftAppService(resolver, sanitizer, agreementDraftRepository,
                agreementRevisionRepository, agreementOperationRepository, agreementTxService);
    }

    @Test
    @DisplayName("新建草稿保存清洗后的安全 HTML 并返回清洗策略版本与调整标记")
    void 新建保存清洗结果() {
        AgreementDraftResult result = service.create(createCmd("<p>正文</p><script>alert(1)</script>"));

        assertEquals("PRIVACY_POLICY", result.getAgreementType());
        assertEquals("DEFAULT", result.getMarket());
        assertEquals("zh-CN", result.getLocale());
        assertEquals("<p>正文</p>", result.getBody());
        assertEquals(AgreementHtmlPolicy.SANITIZER_VERSION, result.getSanitizerVersion());
        assertTrue(result.isAdjusted());
        assertEquals(0, result.getLockVersion());

        ArgumentCaptor<AgreementDraft> draftCaptor = ArgumentCaptor.forClass(AgreementDraft.class);
        verify(agreementDraftRepository).insert(draftCaptor.capture());
        assertEquals("<p>正文</p>", draftCaptor.getValue().getBody());
        verify(agreementOperationRepository).insert(any(AgreementOperation.class));
    }

    @Test
    @DisplayName("范围非法或正文为空时不落库")
    void 非法入参不落库() {
        CreateAgreementDraftCmd unsupported = createCmd("<p>正文</p>");
        unsupported.setLocale("en-US");
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.create(unsupported));

        CreateAgreementDraftCmd emptyBody = createCmd("<script>alert(1)</script>");
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.create(emptyBody));

        CreateAgreementDraftCmd noOperator = createCmd("<p>正文</p>");
        noOperator.setOperator(null);
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.create(noOperator));

        verify(agreementDraftRepository, never()).insert(any());
    }

    @Test
    @DisplayName("修改草稿：乐观锁不匹配拒绝且不覆盖")
    void 乐观锁冲突() {
        when(agreementDraftRepository.findByDraftId("DRF-1")).thenReturn(Optional.of(existingDraft(3)));

        assertCode(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT, () -> service.update(updateCmd(2)));

        assertCode(AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND, () -> service.update(updateCmd(3, "DRF-X")));
        verify(agreementDraftRepository, never()).updateContent(any(), anyInt());
    }

    @Test
    @DisplayName("修改草稿：更新行数为 0 视为并发冲突")
    void 并发更新冲突() {
        when(agreementDraftRepository.findByDraftId("DRF-1")).thenReturn(Optional.of(existingDraft(3)));
        when(agreementDraftRepository.updateContent(any(), eq(3))).thenReturn(0);

        assertCode(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT, () -> service.update(updateCmd(3)));
    }

    @Test
    @DisplayName("修改草稿：正文重新清洗并递增乐观锁版本")
    void 修改成功() {
        when(agreementDraftRepository.findByDraftId("DRF-1")).thenReturn(Optional.of(existingDraft(3)));
        when(agreementDraftRepository.updateContent(any(), eq(3))).thenReturn(1);

        AgreementDraftResult result = service.update(updateCmd(3));

        assertEquals(4, result.getLockVersion());
        assertEquals("<h2>新标题</h2><p>新正文</p>", result.getBody());
        verify(agreementOperationRepository).insert(any(AgreementOperation.class));
    }

    @Test
    @DisplayName("定稿：同一草稿版本重复定稿返回同一修订，不重复生成")
    void 定稿幂等() {
        AgreementRevision existing = revision("REV-EXIST");
        when(agreementRevisionRepository.findBySourceDraft("DRF-1", 3)).thenReturn(Optional.of(existing));

        AgreementRevisionResult result = service.freeze(freezeCmd(3));

        assertEquals("REV-EXIST", result.getRevisionId());
        verify(agreementTxService, never()).freeze(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("定稿：并发唯一键竞争回滚后返回胜出修订")
    void 定稿并发竞争() {
        AgreementRevision winner = revision("REV-WINNER");
        when(agreementRevisionRepository.findBySourceDraft("DRF-1", 3))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(agreementTxService.freeze("DRF-1", 3, "tester")).thenThrow(new DuplicateKeyException("uk_source_draft"));

        AgreementRevisionResult result = service.freeze(freezeCmd(3));

        assertEquals("REV-WINNER", result.getRevisionId());
    }

    @Test
    @DisplayName("定稿：业务版本已被其它正文占用时拒绝")
    void 定稿版本占用() {
        when(agreementRevisionRepository.findBySourceDraft("DRF-1", 3)).thenReturn(Optional.empty());
        when(agreementTxService.freeze("DRF-1", 3, "tester"))
                .thenThrow(new DuplicateKeyException("uk_identity_version"));

        assertCode(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT, () -> service.freeze(freezeCmd(3)));
    }

    @Test
    @DisplayName("定稿必须携带 lockVersion")
    void 定稿必须带版本() {
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.freeze(freezeCmd(null)));
    }

    @Test
    @DisplayName("查询草稿不存在返回草稿不存在错误码")
    void 查询不存在() {
        when(agreementDraftRepository.findByDraftId("DRF-X")).thenReturn(Optional.empty());

        assertCode(AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND, () -> service.get("DRF-X"));
    }

    private CreateAgreementDraftCmd createCmd(String body) {
        return CreateAgreementDraftCmd.builder()
                .agreementType("PRIVACY_POLICY")
                .scene("IVI_ACTIVATION")
                .title("隐私政策")
                .version("1.0")
                .agreementDate(AGREEMENT_DATE)
                .body(body)
                .operator("tester")
                .build();
    }

    private UpdateAgreementDraftCmd updateCmd(int lockVersion) {
        return updateCmd(lockVersion, "DRF-1");
    }

    private UpdateAgreementDraftCmd updateCmd(int lockVersion, String draftId) {
        return UpdateAgreementDraftCmd.builder()
                .draftId(draftId)
                .lockVersion(lockVersion)
                .body("<h2>新标题</h2><p>新正文</p>")
                .operator("tester")
                .build();
    }

    private FreezeAgreementDraftCmd freezeCmd(Integer lockVersion) {
        return FreezeAgreementDraftCmd.builder()
                .draftId("DRF-1")
                .lockVersion(lockVersion)
                .operator("tester")
                .build();
    }

    private AgreementDraft existingDraft(int lockVersion) {
        return AgreementDraft.builder()
                .draftId("DRF-1")
                .type(AgreementType.PRIVACY_POLICY)
                .scope(defaultScope())
                .title("隐私政策")
                .version("1.0")
                .agreementDate(AGREEMENT_DATE)
                .contentType(SanitizedHtml.CONTENT_TYPE)
                .body("<p>旧正文</p>")
                .sanitizerVersion(AgreementHtmlPolicy.SANITIZER_VERSION)
                .lockVersion(lockVersion)
                .createdBy("tester")
                .createdAt(NOW)
                .updatedBy("tester")
                .updatedAt(NOW)
                .build();
    }

    private AgreementRevision revision(String revisionId) {
        return AgreementRevision.builder()
                .revisionId(revisionId)
                .agreementCode("AGR-TEST")
                .type(AgreementType.PRIVACY_POLICY)
                .scope(defaultScope())
                .version("1.0")
                .agreementDate(AGREEMENT_DATE)
                .title("隐私政策")
                .contentType(SanitizedHtml.CONTENT_TYPE)
                .body("<p>正文</p>")
                .contentHash("hash")
                .sanitizerVersion(AgreementHtmlPolicy.SANITIZER_VERSION)
                .sourceDraftId("DRF-1")
                .sourceDraftVersion(3)
                .frozenBy("tester")
                .frozenAt(NOW)
                .build();
    }

    private AgreementScope defaultScope() {
        return AgreementScope.of(net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene.IVI_ACTIVATION,
                "DEFAULT", "zh-CN", "DEFAULT", "DEFAULT");
    }

    private void assertCode(AgreementErrorCode expected, Runnable action) {
        AgreementException exception = assertThrows(AgreementException.class, action::run);
        assertEquals(expected, exception.getErrorCode());
        assertFalse(exception.getMessage() == null || exception.getMessage().isEmpty());
    }

    private static final class DefaultOnlyScopeProvider implements SupportedScopeProvider {
        @Override
        public Set<String> supportedMarkets() {
            return Set.of("DEFAULT");
        }

        @Override
        public Set<String> supportedLocales() {
            return Set.of("zh-CN");
        }

        @Override
        public Set<String> supportedBrands() {
            return Set.of("DEFAULT");
        }

        @Override
        public Set<String> supportedCarLines() {
            return Set.of("DEFAULT");
        }

        @Override
        public boolean isCarLineOwnedByBrand(String brandCode, String carLineCode) {
            return "DEFAULT".equals(carLineCode);
        }
    }
}
