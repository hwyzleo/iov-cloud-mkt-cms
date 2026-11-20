package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.service;

import net.hwyz.iov.cloud.mkt.cms.service.support.JsonTestSupport;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementScopeQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleContentResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 协议内容内部服务接口契约测试（MKT-CMS-DSN-CR-001 §6 / AT-005 / AT-006）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("服务端协议读取 - no-store/固定两项/未发布不可读")
class ServiceAgreementContentControllerTest {

    private static final String BASE = "/api/service/content/v1";

    @Mock
    private AgreementContentAppService agreementContentAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ServiceAgreementContentController controller =
                new ServiceAgreementContentController(agreementContentAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonTestSupport.objectMapper()))
                .build();
    }

    @Test
    @DisplayName("当前包：仅传场景即按补齐范围读取，返回完整范围与两份正文，响应 no-store")
    void 当前包读取() throws Exception {
        when(agreementContentAppService.getCurrentBundle(any())).thenReturn(bundleContentResult());

        mockMvc.perform(get(BASE + "/agreement-bundles/current").param("scene", "IVI_ACTIVATION"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.bundleId").value("BDL-1"))
                .andExpect(jsonPath("$.market").value("DEFAULT"))
                .andExpect(jsonPath("$.locale").value("zh-CN"))
                .andExpect(jsonPath("$.brandCode").value("DEFAULT"))
                .andExpect(jsonPath("$.carLineCode").value("DEFAULT"))
                .andExpect(jsonPath("$.agreements.length()").value(2))
                .andExpect(jsonPath("$.agreements[0].contentType").value("text/html; charset=utf-8"))
                .andExpect(jsonPath("$.agreements[0].sanitizerVersion").value("html-allowlist-v1"))
                .andExpect(jsonPath("$.agreements[0].contentHash").value("sha256:hash-privacy"))
                .andExpect(jsonPath("$.agreements[1].agreementType").value("USER_AGREEMENT"));

        ArgumentCaptor<AgreementScopeQuery> captor = ArgumentCaptor.forClass(AgreementScopeQuery.class);
        verify(agreementContentAppService).getCurrentBundle(captor.capture());
        assertEquals("IVI_ACTIVATION", captor.getValue().getScene());
    }

    @Test
    @DisplayName("无当前包返回 404 与明确错误码，不返回空包")
    void 无当前包() throws Exception {
        when(agreementContentAppService.getCurrentBundle(any()))
                .thenThrow(new AgreementException(AgreementErrorCode.AGREEMENT_CURRENT_NOT_FOUND));

        mockMvc.perform(get(BASE + "/agreement-bundles/current").param("scene", "IVI_ACTIVATION"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("105002"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("缺少必传场景返回 400")
    void 缺少场景() throws Exception {
        mockMvc.perform(get(BASE + "/agreement-bundles/current"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("105001"));
    }

    @Test
    @DisplayName("历史修订：返回单份协议内容结构")
    void 历史修订读取() throws Exception {
        when(agreementContentAppService.getPublishedRevision("REV-P"))
                .thenReturn(contentResult("PRIVACY_POLICY", "REV-P", "hash-privacy"));

        mockMvc.perform(get(BASE + "/agreement-revisions/REV-P"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionId").value("REV-P"))
                .andExpect(jsonPath("$.agreementType").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.agreementDate").value("2026-11-20"))
                .andExpect(jsonPath("$.contentHash").value("sha256:hash-privacy"))
                .andExpect(jsonPath("$.body").value("<p>正文</p>"));
    }

    @Test
    @DisplayName("未发布修订对业务读取不可读，返回 404")
    void 未发布修订() throws Exception {
        when(agreementContentAppService.getPublishedRevision("REV-DRAFT"))
                .thenThrow(new AgreementException(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND));

        mockMvc.perform(get(BASE + "/agreement-revisions/REV-DRAFT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("105003"));
    }

    private AgreementBundleContentResult bundleContentResult() {
        return AgreementBundleContentResult.builder()
                .bundleId("BDL-1")
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .brandCode("DEFAULT")
                .carLineCode("DEFAULT")
                .publishedAt(Instant.parse("2026-11-20T02:00:00Z"))
                .agreements(Arrays.asList(
                        contentResult("PRIVACY_POLICY", "REV-P", "hash-privacy"),
                        contentResult("USER_AGREEMENT", "REV-U", "hash-user")))
                .build();
    }

    private AgreementBundleContentResult.AgreementContentResult contentResult(String type, String revisionId,
                                                                             String hash) {
        return AgreementBundleContentResult.AgreementContentResult.builder()
                .agreementCode("AGR-" + type)
                .agreementType(type)
                .revisionId(revisionId)
                .version("1.0")
                .agreementDate(LocalDate.of(2026, 11, 20))
                .title("协议")
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .brandCode("DEFAULT")
                .carLineCode("DEFAULT")
                .contentType("text/html; charset=utf-8")
                .body("<p>正文</p>")
                .contentHash("sha256:" + hash)
                .sanitizerVersion("html-allowlist-v1")
                .build();
    }
}
