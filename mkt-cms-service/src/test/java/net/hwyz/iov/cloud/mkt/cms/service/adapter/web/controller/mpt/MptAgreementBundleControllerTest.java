package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import com.github.pagehelper.Page;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.PublishAgreementBundleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementScopeQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleContentResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleSummaryResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementPublishAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.support.JsonTestSupport;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 协议发布接口契约测试（MKT-CMS-DSN-CR-001 / CR-002 / AT-003 / AT-004）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MPT 协议发布与历史包 - 前提字段/权限点/分页/冲突语义")
class MptAgreementBundleControllerTest {

    private static final String BASE = "/api/mpt/agreement/v1/agreement-bundles";

    @Mock
    private AgreementPublishAppService agreementPublishAppService;
    @Mock
    private AgreementContentAppService agreementContentAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MptAgreementBundleController controller =
                new MptAgreementBundleController(agreementPublishAppService, agreementContentAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonTestSupport.objectMapper()))
                .build();
    }

    @Test
    @DisplayName("显式 expectedBundleId=null 表示首次发布，字段存在性被记录")
    void 首次发布显式空前提() throws Exception {
        when(agreementPublishAppService.publish(any())).thenReturn(bundleResult());

        mockMvc.perform(post(BASE + "/action/publish").contentType(MediaType.APPLICATION_JSON)
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

        mockMvc.perform(post(BASE + "/action/publish").contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("")))
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

        mockMvc.perform(post(BASE + "/action/publish").contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("\"expectedBundleId\":\"BDL-OLD\",")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("105006"));
    }

    @Test
    @DisplayName("历史发布包分页：total 来自分页插件，范围与语言条件透传")
    void 历史发布包分页() throws Exception {
        Page<AgreementBundleSummaryResult> page = new Page<>(1, 20, true);
        page.setTotal(7L);
        page.add(AgreementBundleSummaryResult.builder()
                .bundleId("BDL-1")
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .brandCode("DEFAULT")
                .carLineCode("DEFAULT")
                .privacyRevisionId("REV-P")
                .userRevisionId("REV-U")
                .publishedBy("publisher")
                .publishedAt(Instant.parse("2026-11-20T02:00:00Z"))
                .requestId("req-1")
                .build());
        when(agreementContentAppService.searchBundles(any())).thenReturn(page);

        mockMvc.perform(get(BASE + "/list").param("scene", "IVI_ACTIVATION")
                        .param("locale", "zh-CN").param("pageNum", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(7))
                .andExpect(jsonPath("$.data.items[0].bundleId").value("BDL-1"))
                .andExpect(jsonPath("$.data.items[0].privacyRevisionId").value("REV-P"));
    }

    @Test
    @DisplayName("管理侧当前包返回两份协议正文与摘要")
    void 管理侧当前包() throws Exception {
        when(agreementContentAppService.getCurrentBundle(any())).thenReturn(currentBundleResult());

        mockMvc.perform(get(BASE + "/current").param("scene", "IVI_ACTIVATION").param("locale", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bundleId").value("BDL-1"))
                .andExpect(jsonPath("$.data.locale").value("zh-CN"))
                .andExpect(jsonPath("$.data.agreements.length()").value(2))
                .andExpect(jsonPath("$.data.agreements[0].contentHash").value("sha256:hash-privacy"));

        ArgumentCaptor<AgreementScopeQuery> captor = ArgumentCaptor.forClass(AgreementScopeQuery.class);
        verify(agreementContentAppService).getCurrentBundle(captor.capture());
        assertEquals("zh-CN", captor.getValue().getLocale());
    }

    @Test
    @DisplayName("无当前包返回 404")
    void 无当前包() throws Exception {
        when(agreementContentAppService.getCurrentBundle(any()))
                .thenThrow(new AgreementException(AgreementErrorCode.AGREEMENT_CURRENT_NOT_FOUND));

        mockMvc.perform(get(BASE + "/current").param("scene", "IVI_ACTIVATION"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("105002"));
    }

    @Test
    @DisplayName("发布、历史列表与当前包端点必须声明权限点")
    void 权限点声明() throws Exception {
        assertPermission("publish", PostMapping.class, "mkt:cms:agreement:publish");
        assertPermission("list", GetMapping.class, "mkt:cms:agreement:query");
        assertPermission("current", GetMapping.class, "mkt:cms:agreement:query");
    }

    private void assertPermission(String methodName, Class<? extends java.lang.annotation.Annotation> mappingType,
                                  String expectedPermission) throws Exception {
        Method method = java.util.Arrays.stream(MptAgreementBundleController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("方法不存在: " + methodName));
        assertNotNull(method.getAnnotation(mappingType), methodName + " 路由注解缺失");
        RequiresPermissions permission = method.getAnnotation(RequiresPermissions.class);
        assertNotNull(permission, methodName + " 权限点缺失");
        assertTrue(java.util.Arrays.asList(permission.value()).contains(expectedPermission),
                methodName + " 权限点不匹配");
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

    private AgreementBundleContentResult currentBundleResult() {
        return AgreementBundleContentResult.builder()
                .bundleId("BDL-1")
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .brandCode("DEFAULT")
                .carLineCode("DEFAULT")
                .publishedAt(Instant.parse("2026-11-20T02:00:00Z"))
                .agreements(List.of(
                        contentItem("PRIVACY_POLICY", "REV-P", "hash-privacy"),
                        contentItem("USER_AGREEMENT", "REV-U", "hash-user")))
                .build();
    }

    private AgreementBundleContentResult.AgreementContentResult contentItem(String type, String revisionId,
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
