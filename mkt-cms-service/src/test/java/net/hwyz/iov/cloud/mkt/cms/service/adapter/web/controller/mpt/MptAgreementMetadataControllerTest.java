package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import com.github.pagehelper.Page;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementSearchQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementMetadataResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionSummaryResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementMetadataAppService;
import net.hwyz.iov.cloud.mkt.cms.service.support.JsonTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 协议管理元数据与修订列表契约测试（MKT-CMS-DSN-CR-002 / 多语言扩展）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MPT 协议元数据与修订列表 - 受支持清单/分页/语言过滤")
class MptAgreementMetadataControllerTest {

    @Mock
    private AgreementMetadataAppService agreementMetadataAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MptAgreementMetadataController metadataController =
                new MptAgreementMetadataController(agreementMetadataAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(metadataController)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonTestSupport.objectMapper()))
                .build();
    }

    @Test
    @DisplayName("元数据下发受支持语言/市场/品牌/车系与品牌车系归属")
    void 元数据下发() throws Exception {
        when(agreementMetadataAppService.metadata()).thenReturn(AgreementMetadataResult.builder()
                .agreementTypes(List.of("PRIVACY_POLICY", "USER_AGREEMENT"))
                .scenes(List.of("IVI_ACTIVATION"))
                .markets(List.of("DEFAULT"))
                .locales(List.of("zh-CN"))
                .brands(List.of("DEFAULT", "BM"))
                .carLines(List.of("DEFAULT", "CL1"))
                .carLinesByBrand(Map.of("BM", List.of("CL1")))
                .contentType("text/html; charset=utf-8")
                .sanitizerVersion("html-allowlist-v1")
                .maxInputBytes(1048576)
                .maxOutputBytes(1048576)
                .build());

        mockMvc.perform(get("/api/mpt/agreement/v1/agreement-metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locales[0]").value("zh-CN"))
                .andExpect(jsonPath("$.data.carLinesByBrand.BM[0]").value("CL1"))
                .andExpect(jsonPath("$.data.sanitizerVersion").value("html-allowlist-v1"))
                .andExpect(jsonPath("$.data.maxInputBytes").value(1048576));
    }

    @Test
    @DisplayName("修订分页：语言与已发布条件透传，列表不含正文")
    void 修订分页与语言过滤() throws Exception {
        AgreementContentAppService contentAppService = org.mockito.Mockito.mock(AgreementContentAppService.class);
        MptAgreementRevisionController revisionController = new MptAgreementRevisionController(contentAppService);
        MockMvc revisionMvc = MockMvcBuilders.standaloneSetup(revisionController)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonTestSupport.objectMapper()))
                .build();

        Page<AgreementRevisionSummaryResult> page = new Page<>(1, 10, true);
        page.setTotal(3L);
        page.add(AgreementRevisionSummaryResult.builder()
                .revisionId("REV-P")
                .agreementCode("AGR-PRIVACY_POLICY-IVI_ACTIVATION-ABCD1234")
                .agreementType("PRIVACY_POLICY")
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .brandCode("DEFAULT")
                .carLineCode("DEFAULT")
                .title("隐私政策")
                .version("1.0")
                .agreementDate(LocalDate.of(2026, 11, 20))
                .contentHash("sha256:hash")
                .sanitizerVersion("html-allowlist-v1")
                .sourceDraftVersion(2)
                .frozenBy("tester")
                .frozenAt(Instant.parse("2026-11-20T02:00:00Z"))
                .build());
        when(contentAppService.searchRevisions(any())).thenReturn(page);

        revisionMvc.perform(get("/api/mpt/agreement/v1/agreement-revisions/list")
                        .param("locale", "zh-CN").param("published", "true")
                        .param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[0].revisionId").value("REV-P"))
                .andExpect(jsonPath("$.data.items[0].contentHash").value("sha256:hash"));

        ArgumentCaptor<AgreementSearchQuery> captor = ArgumentCaptor.forClass(AgreementSearchQuery.class);
        verify(contentAppService).searchRevisions(captor.capture());
        assertEquals("zh-CN", captor.getValue().getLocale());
        assertTrue(captor.getValue().getPublished());

        java.lang.reflect.Method listMethod = java.util.Arrays.stream(
                        MptAgreementRevisionController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("list"))
                .findFirst()
                .orElseThrow();
        assertTrue(listMethod.isAnnotationPresent(net.hwyz.iov.cloud.framework.security.annotation
                .RequiresPermissions.class));
    }
}
