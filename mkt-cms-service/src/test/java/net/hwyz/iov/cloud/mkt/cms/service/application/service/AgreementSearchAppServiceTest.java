package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import com.github.pagehelper.Page;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementSearchQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleSummaryResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementDraftSummaryResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementMetadataResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionSummaryResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementBundleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementDraftRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 协议管理检索与元数据应用服务单元测试（MKT-CMS-DSN-CR-002）。
 * <p>
 * 覆盖：检索条件规范化（空串视为不过滤）、分页元信息保留、元数据受支持清单下发。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("协议管理检索 - 条件规范化/分页/元数据")
class AgreementSearchAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");

    private static final AgreementScope SCOPE = AgreementScope.of(AgreementScene.IVI_ACTIVATION,
            "DEFAULT", "zh-CN", "DEFAULT", "DEFAULT");

    @Mock
    private AgreementDraftRepository agreementDraftRepository;
    @Mock
    private AgreementRevisionRepository agreementRevisionRepository;
    @Mock
    private AgreementBundleRepository agreementBundleRepository;

    private AgreementDraftAppService draftAppService;
    private AgreementContentAppService contentAppService;

    @BeforeEach
    void setUp() {
        draftAppService = new AgreementDraftAppService(new ScopeResolver(new StubScopeProvider()),
                new JsoupHtmlSanitizer(new AgreementHtmlPolicy(1024, 1024)), agreementDraftRepository,
                agreementRevisionRepository, org.mockito.Mockito.mock(
                net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementOperationRepository.class),
                org.mockito.Mockito.mock(AgreementTxService.class));
        contentAppService = new AgreementContentAppService(new ScopeResolver(new StubScopeProvider()),
                agreementBundleRepository, agreementRevisionRepository);
    }

    @Test
    @DisplayName("草稿检索：空串条件视为不过滤，分页总数保留，列表项不含正文")
    void 草稿检索条件规范化() {
        Page<AgreementDraft> page = new Page<>(1, 20, true);
        page.setTotal(12L);
        page.add(draft());
        when(agreementDraftRepository.search(any())).thenReturn(page);

        List<AgreementDraftSummaryResult> result = draftAppService.search(AgreementSearchQuery.builder()
                .agreementType("PRIVACY_POLICY")
                .locale("  ")
                .title(" 隐私 ")
                .build());

        ArgumentCaptor<AgreementSearchCriteria> captor = ArgumentCaptor.forClass(AgreementSearchCriteria.class);
        verify(agreementDraftRepository).search(captor.capture());
        assertNull(captor.getValue().getLocale());
        assertEquals("隐私", captor.getValue().getTitle());
        assertEquals("PRIVACY_POLICY", captor.getValue().getAgreementType());

        assertEquals(1, result.size());
        assertEquals("DRF-1", result.get(0).getDraftId());
        assertEquals("zh-CN", result.get(0).getLocale());
        assertTrue(result instanceof Page);
        assertEquals(12L, ((Page<?>) result).getTotal());
    }

    @Test
    @DisplayName("修订检索：已发布条件与语言条件透传")
    void 修订检索条件() {
        Page<AgreementRevision> page = new Page<>(1, 20, true);
        page.setTotal(1L);
        page.add(revision());
        when(agreementRevisionRepository.search(any())).thenReturn(page);

        List<AgreementRevisionSummaryResult> result = contentAppService.searchRevisions(AgreementSearchQuery.builder()
                .locale("zh-CN")
                .published(true)
                .build());

        ArgumentCaptor<AgreementSearchCriteria> captor = ArgumentCaptor.forClass(AgreementSearchCriteria.class);
        verify(agreementRevisionRepository).search(captor.capture());
        assertEquals("zh-CN", captor.getValue().getLocale());
        assertEquals(Boolean.TRUE, captor.getValue().getPublished());
        assertEquals(1, result.size());
        assertEquals("REV-1", result.get(0).getRevisionId());
        assertEquals("sha256:hash", result.get(0).getContentHash());
    }

    @Test
    @DisplayName("发布包检索：范围条件透传")
    void 发布包检索条件() {
        Page<AgreementBundle> page = new Page<>(1, 20, true);
        page.setTotal(2L);
        page.add(bundle());
        when(agreementBundleRepository.search(any())).thenReturn(page);

        List<AgreementBundleSummaryResult> result = contentAppService.searchBundles(AgreementSearchQuery.builder()
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .build());

        assertEquals(1, result.size());
        assertEquals("BDL-1", result.get(0).getBundleId());
        assertEquals("REV-P", result.get(0).getPrivacyRevisionId());
    }

    @Test
    @DisplayName("元数据：下发受支持语言/市场/品牌/车系与品牌车系归属")
    void 元数据下发() {
        AgreementMetadataAppService metadataAppService =
                new AgreementMetadataAppService(new StubScopeProvider(),
                        new AgreementHtmlPolicy(1048576, 1048576));

        AgreementMetadataResult metadata = metadataAppService.metadata();

        assertEquals(List.of("PRIVACY_POLICY", "USER_AGREEMENT"), metadata.getAgreementTypes());
        assertEquals(List.of("IVI_ACTIVATION"), metadata.getScenes());
        assertEquals(List.of("DEFAULT"), metadata.getMarkets());
        assertEquals(List.of("zh-CN"), metadata.getLocales());
        assertEquals(List.of("BM", "DEFAULT"), metadata.getBrands());
        assertEquals(List.of("CL1"), metadata.getCarLinesByBrand().get("BM"));
        assertEquals("html-allowlist-v1", metadata.getSanitizerVersion());
        assertEquals(SanitizedHtml.CONTENT_TYPE, metadata.getContentType());
        assertEquals(1048576, metadata.getMaxInputBytes());
    }

    private AgreementDraft draft() {
        return AgreementDraft.builder()
                .draftId("DRF-1")
                .type(AgreementType.PRIVACY_POLICY)
                .scope(SCOPE)
                .title("隐私政策")
                .version("1.0")
                .agreementDate(LocalDate.of(2026, 11, 20))
                .contentType(SanitizedHtml.CONTENT_TYPE)
                .sanitizerVersion(AgreementHtmlPolicy.SANITIZER_VERSION)
                .lockVersion(2)
                .updatedBy("tester")
                .updatedAt(NOW)
                .build();
    }

    private AgreementRevision revision() {
        return AgreementRevision.builder()
                .revisionId("REV-1")
                .agreementCode("AGR-PRIVACY_POLICY-IVI_ACTIVATION-ABCD1234")
                .type(AgreementType.PRIVACY_POLICY)
                .scope(SCOPE)
                .title("隐私政策")
                .version("1.0")
                .agreementDate(LocalDate.of(2026, 11, 20))
                .contentType(SanitizedHtml.CONTENT_TYPE)
                .contentHash("hash")
                .sanitizerVersion(AgreementHtmlPolicy.SANITIZER_VERSION)
                .sourceDraftVersion(2)
                .frozenBy("tester")
                .frozenAt(NOW)
                .build();
    }

    private AgreementBundle bundle() {
        return AgreementBundle.builder()
                .bundleId("BDL-1")
                .scope(SCOPE)
                .privacyRevisionId("REV-P")
                .userRevisionId("REV-U")
                .publishedBy("publisher")
                .publishedAt(NOW)
                .requestId("req-1")
                .build();
    }

    private static final class StubScopeProvider implements SupportedScopeProvider {

        private final Map<String, List<String>> carLinesByBrand = new LinkedHashMap<>();

        private StubScopeProvider() {
            carLinesByBrand.put("BM", List.of("CL1"));
        }

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
            return Set.of("DEFAULT", "BM");
        }

        @Override
        public Set<String> supportedCarLines() {
            return Set.of("DEFAULT", "CL1");
        }

        @Override
        public boolean isCarLineOwnedByBrand(String brandCode, String carLineCode) {
            return carLinesByBrand.getOrDefault(brandCode, List.of()).contains(carLineCode)
                    || "DEFAULT".equals(carLineCode);
        }

        @Override
        public Map<String, List<String>> carLinesByBrand() {
            return carLinesByBrand;
        }
    }
}
