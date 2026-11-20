package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.mkt.cms.service.support.JsonTestSupport;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.PublishAgreementBundleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementPublishAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 协议发布接口契约测试（MKT-CMS-DSN-CR-001 §5 / AT-003 / AT-004）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MPT 协议发布 - 前提字段/权限点/冲突语义")
class MptAgreementBundleControllerTest {

    private static final String BASE = "/api/mpt/agreement/v1/agreement-bundles/action/publish";

    @Mock
    private AgreementPublishAppService agreementPublishAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MptAgreementBundleController controller = new MptAgreementBundleController(agreementPublishAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonTestSupport.objectMapper()))
                .build();
    }

    @Test
    @DisplayName("显式 expectedBundleId=null 表示首次发布，字段存在性被记录")
    void 首次发布显式空前提() throws Exception {
        when(agreementPublishAppService.publish(any())).thenReturn(bundleResult());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("\"expectedBundleId\":null,")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bundleId").value("BDL-1"))
                .andExpect(jsonPath("$.data.requestId").value("req-1"));

        ArgumentCaptor<PublishAgreementBundleCmd> captor = ArgumentCaptor.forClass(PublishAgreementBundleCmd.class);
        verify(agreementPublishAppService).publish(captor.capture());
        assertTrue(captor.getValue().isExpectedBundleIdPresent());
        assertEquals(null, captor.getValue().getExpectedBundleId());
    }

    @Test
    @DisplayName("缺失 expectedBundleId 字段时不被当作显式 null")
    void 缺失前提字段() throws Exception {
        when(agreementPublishAppService.publish(any()))
                .thenThrow(new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                        "expectedBundleId 字段必须出现（首次发布请显式传 null）"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(requestJson("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("105001"));

        ArgumentCaptor<PublishAgreementBundleCmd> captor = ArgumentCaptor.forClass(PublishAgreementBundleCmd.class);
        verify(agreementPublishAppService).publish(captor.capture());
        assertFalse(captor.getValue().isExpectedBundleIdPresent());
    }

    @Test
    @DisplayName("当前包已变化返回 409 与发布冲突错误码")
    void 发布冲突语义() throws Exception {
        when(agreementPublishAppService.publish(any()))
                .thenThrow(new AgreementException(AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("\"expectedBundleId\":\"BDL-OLD\",")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("105006"));
    }

    @Test
    @DisplayName("发布端点必须声明发布权限点")
    void 权限点声明() throws Exception {
        Method method = MptAgreementBundleController.class.getDeclaredMethod(
                "publish", net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request
                        .PublishAgreementBundleRequest.class);
        assertNotNull(method.getAnnotation(PostMapping.class));
        RequiresPermissions permission = method.getAnnotation(RequiresPermissions.class);
        assertNotNull(permission);
        assertTrue(java.util.Arrays.asList(permission.value()).contains("mkt:cms:agreement:publish"));
    }

    private String requestJson(String expectedBundleIdField) {
        return "{" + expectedBundleIdField
                + "\"scene\":\"IVI_ACTIVATION\",\"privacyRevisionId\":\"REV-P\","
                + "\"userAgreementRevisionId\":\"REV-U\",\"requestId\":\"req-1\"}";
    }

    private AgreementBundleResult bundleResult() {
        return AgreementBundleResult.builder()
                .bundleId("BDL-1")
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .brandCode("DEFAULT")
                .carLineCode("DEFAULT")
                .privacyRevisionId("REV-P")
                .userRevisionId("REV-U")
                .publishedBy("tester")
                .publishedAt(Instant.parse("2026-11-20T02:00:00Z"))
                .requestId("req-1")
                .requestHash("hash")
                .build();
    }
}
