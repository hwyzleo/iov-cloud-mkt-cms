package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementScopeQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleContentResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementBundleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementRevisionRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 协议内容读取应用服务单元测试（MKT-CMS-DSN-CR-001 §6 / AT-005 / AT-006）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("协议读取 - 精确范围/无回退/未发布不可读")
class AgreementContentAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");
    private static final AgreementScope DEFAULT_SCOPE = AgreementScope.of(AgreementScene.IVI_ACTIVATION,
            "DEFAULT", "zh-CN", "DEFAULT", "DEFAULT");

    @Mock
    private AgreementBundleRepository agreementBundleRepository;
    @Mock
    private AgreementRevisionRepository agreementRevisionRepository;

    private AgreementContentAppService service;

    @BeforeEach
    void setUp() {
        service = new AgreementContentAppService(new ScopeResolver(new DefaultOnlyScopeProvider()),
                agreementBundleRepository, agreementRevisionRepository);
    }

    @Test
    @DisplayName("仅传场景：按补齐后的完整范围精确查询，返回固定两项协议")
    void 默认范围读取当前包() {
        when(agreementBundleRepository.findCurrentView(DEFAULT_SCOPE))
                .thenReturn(Optional.of(net.hwyz.iov.cloud.mkt.cms.service.domain.model.CurrentBundleView.builder()
                        .bundleId("BDL-1")
                        .scope(DEFAULT_SCOPE)
                        .publishedAt(NOW)
                        .privacyPolicy(revision(AgreementType.PRIVACY_POLICY, "REV-P", "隐私政策"))
                        .userAgreement(revision(AgreementType.USER_AGREEMENT, "REV-U", "用户协议"))
                        .build()));

        AgreementBundleContentResult result = service.getCurrentBundle(query(null, null, null, null));

        assertEquals("BDL-1", result.getBundleId());
        assertEquals("DEFAULT", result.getMarket());
        assertEquals("zh-CN", result.getLocale());
        assertEquals(2, result.getAgreements().size());
        assertEquals("PRIVACY_POLICY", result.getAgreements().get(0).getAgreementType());
        assertEquals("USER_AGREEMENT", result.getAgreements().get(1).getAgreementType());
        assertEquals("sha256:hash-privacy", result.getAgreements().get(0).getContentHash());
    }

    @Test
    @DisplayName("范围合法但没有当前包：明确失败，不回退不返回空包")
    void 无当前包() {
        when(agreementBundleRepository.findCurrentView(any())).thenReturn(Optional.empty());

        assertCode(AgreementErrorCode.AGREEMENT_CURRENT_NOT_FOUND,
                () -> service.getCurrentBundle(query(null, null, null, null)));
    }

    @Test
    @DisplayName("显式非法范围拒绝，不查库")
    void 非法范围() {
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                () -> service.getCurrentBundle(query("en-US", null, null, null)));
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                () -> service.getCurrentBundle(query(null, "CN", null, null)));
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                () -> service.getCurrentBundle(query(null, null, "DEFAULT", "CL1")));
        verify(agreementBundleRepository, never()).findCurrentView(any());
    }

    @Test
    @DisplayName("历史修订：仅被发布包引用时可读，未发布视为不可读")
    void 历史修订可读性() {
        when(agreementRevisionRepository.findPublishedByRevisionId("REV-P"))
                .thenReturn(Optional.of(revision(AgreementType.PRIVACY_POLICY, "REV-P", "隐私政策")));
        when(agreementRevisionRepository.findPublishedByRevisionId("REV-DRAFT"))
                .thenReturn(Optional.empty());

        AgreementBundleContentResult.AgreementContentResult published = service.getPublishedRevision("REV-P");
        assertEquals("REV-P", published.getRevisionId());
        assertEquals("隐私政策", published.getTitle());
        assertEquals("sha256:hash-privacy", published.getContentHash());

        assertCode(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND,
                () -> service.getPublishedRevision("REV-DRAFT"));
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.getPublishedRevision(" "));
    }

    @Test
    @DisplayName("管理侧可查看未发布修订")
    void 管理侧修订可见() {
        when(agreementRevisionRepository.findByRevisionId("REV-P"))
                .thenReturn(Optional.of(revision(AgreementType.PRIVACY_POLICY, "REV-P", "隐私政策")));
        when(agreementRevisionRepository.findByRevisionId("REV-NONE")).thenReturn(Optional.empty());

        assertEquals("REV-P", service.getRevisionForManagement("REV-P").getRevisionId());
        assertCode(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND,
                () -> service.getRevisionForManagement("REV-NONE"));
    }

    private AgreementScopeQuery query(String locale, String market, String brand, String carLine) {
        return AgreementScopeQuery.builder()
                .scene("IVI_ACTIVATION")
                .market(market)
                .locale(locale)
                .brandCode(brand)
                .carLineCode(carLine)
                .build();
    }

    private AgreementRevision revision(AgreementType type, String revisionId, String title) {
        return AgreementRevision.builder()
                .revisionId(revisionId)
                .agreementCode("AGR-" + type.name())
                .type(type)
                .scope(DEFAULT_SCOPE)
                .version("1.0")
                .agreementDate(LocalDate.of(2026, 11, 20))
                .title(title)
                .contentType(SanitizedHtml.CONTENT_TYPE)
                .body("<p>" + title + "</p>")
                .contentHash(type == AgreementType.PRIVACY_POLICY ? "hash-privacy" : "hash-user")
                .sanitizerVersion(AgreementHtmlPolicy.SANITIZER_VERSION)
                .sourceDraftId("DRF-1")
                .sourceDraftVersion(1)
                .frozenBy("tester")
                .frozenAt(NOW)
                .build();
    }

    private void assertCode(AgreementErrorCode expected, Runnable action) {
        AgreementException exception = assertThrows(AgreementException.class, action::run);
        assertEquals(expected, exception.getErrorCode());
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
