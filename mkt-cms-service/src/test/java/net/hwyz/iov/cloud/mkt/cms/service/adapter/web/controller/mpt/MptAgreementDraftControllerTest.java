package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.mkt.cms.service.support.JsonTestSupport;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler.AgreementExceptionHandler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.CreateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.FreezeAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.UpdateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementDraftResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementDraftAppService;
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
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 协议草稿管理接口契约测试（MKT-CMS-DSN-CR-001 §4 / AT-001 / AT-008）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MPT 协议草稿 - 路由/权限点/字段/错误语义")
class MptAgreementDraftControllerTest {

    private static final String BASE = "/api/mpt/agreement/v1/agreement-drafts";

    @Mock
    private AgreementDraftAppService agreementDraftAppService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MptAgreementDraftController controller = new MptAgreementDraftController(agreementDraftAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AgreementExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(JsonTestSupport.objectMapper()))
                .build();
    }

    @Test
    @DisplayName("新建草稿返回清洗后正文、清洗策略版本与调整标记")
    void 新建草稿契约() throws Exception {
        when(agreementDraftAppService.create(any())).thenReturn(draftResult("<p>正文</p>", true));

        String body = "{\"agreementType\":\"PRIVACY_POLICY\",\"scene\":\"IVI_ACTIVATION\","
                + "\"title\":\"隐私政策\",\"version\":\"1.0\",\"agreementDate\":\"2026-11-20\","
                + "\"body\":\"<p>正文</p><script>alert(1)</script>\"}";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("000000"))
                .andExpect(jsonPath("$.data.draftId").value("DRF-1"))
                .andExpect(jsonPath("$.data.market").value("DEFAULT"))
                .andExpect(jsonPath("$.data.locale").value("zh-CN"))
                .andExpect(jsonPath("$.data.contentType").value("text/html; charset=utf-8"))
                .andExpect(jsonPath("$.data.sanitizerVersion").value("html-allowlist-v1"))
                .andExpect(jsonPath("$.data.adjusted").value(true))
                .andExpect(jsonPath("$.data.body").value("<p>正文</p>"));

        ArgumentCaptor<CreateAgreementDraftCmd> captor = ArgumentCaptor.forClass(CreateAgreementDraftCmd.class);
        verify(agreementDraftAppService).create(captor.capture());
        assertEquals("IVI_ACTIVATION", captor.getValue().getScene());
    }

    @Test
    @DisplayName("请求字段校验失败返回 400 与参数错误码")
    void 字段校验失败() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scene\":\"IVI_ACTIVATION\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("105001"));
    }

    @Test
    @DisplayName("乐观锁冲突返回 409 与版本冲突错误码")
    void 乐观锁冲突语义() throws Exception {
        when(agreementDraftAppService.update(any()))
                .thenThrow(new AgreementException(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT, "冲突"));

        mockMvc.perform(put(BASE + "/DRF-1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockVersion\":2,\"body\":\"<p>正文</p>\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("105005"));
    }

    @Test
    @DisplayName("草稿不存在返回 404")
    void 草稿不存在() throws Exception {
        when(agreementDraftAppService.get("DRF-X"))
                .thenThrow(new AgreementException(AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND));

        mockMvc.perform(get(BASE + "/DRF-X"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("105004"));
    }

    @Test
    @DisplayName("定稿返回不可变修订与摘要")
    void 定稿契约() throws Exception {
        when(agreementDraftAppService.freeze(any())).thenReturn(revisionResult());

        mockMvc.perform(post(BASE + "/DRF-1/action/freeze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockVersion\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revisionId").value("REV-1"))
                .andExpect(jsonPath("$.data.contentHash").value("sha256:abc"))
                .andExpect(jsonPath("$.data.sourceDraftVersion").value(2));

        ArgumentCaptor<FreezeAgreementDraftCmd> captor = ArgumentCaptor.forClass(FreezeAgreementDraftCmd.class);
        verify(agreementDraftAppService).freeze(captor.capture());
        assertEquals(2, captor.getValue().getLockVersion());
    }

    @Test
    @DisplayName("管理端点必须声明权限点，且不得存在无权限点的方法")
    void 权限点声明() throws Exception {
        assertPermission("create", PostMapping.class, "mkt:cms:agreement:add");
        assertPermission("update", PutMapping.class, "mkt:cms:agreement:edit");
        assertPermission("get", org.springframework.web.bind.annotation.GetMapping.class,
                "mkt:cms:agreement:query");
        assertPermission("freeze", PostMapping.class, "mkt:cms:agreement:freeze");
        assertPermission("list", org.springframework.web.bind.annotation.GetMapping.class,
                "mkt:cms:agreement:query");

        for (Method method : MptAgreementDraftController.class.getDeclaredMethods()) {
            if (method.getAnnotation(PostMapping.class) == null
                    && method.getAnnotation(PutMapping.class) == null
                    && method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class) == null) {
                continue;
            }
            assertNotNull(method.getAnnotation(RequiresPermissions.class),
                    "管理端点缺少权限点: " + method.getName());
        }
    }

    private <T extends java.lang.annotation.Annotation> void assertPermission(String methodName,
                                                                              Class<T> mappingType,
                                                                              String expectedPermission)
            throws Exception {
        Method method = MptAgreementDraftController.class.getDeclaredMethod(methodName, methodParameters(methodName));
        assertNotNull(method.getAnnotation(mappingType), methodName + " 路由注解缺失");
        RequiresPermissions permission = method.getAnnotation(RequiresPermissions.class);
        assertNotNull(permission, methodName + " 权限点缺失");
        assertTrue(java.util.Arrays.asList(permission.value()).contains(expectedPermission),
                methodName + " 权限点不匹配: " + java.util.Arrays.toString(permission.value()));
    }

    private Class<?>[] methodParameters(String methodName) {
        switch (methodName) {
            case "create":
                return new Class<?>[]{net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request
                        .CreateAgreementDraftRequest.class};
            case "update":
                return new Class<?>[]{String.class, net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request
                        .UpdateAgreementDraftRequest.class};
            case "get":
                return new Class<?>[]{String.class};
            case "freeze":
                return new Class<?>[]{String.class, net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request
                        .FreezeAgreementDraftRequest.class};
            case "list":
                return new Class<?>[]{String.class, String.class, String.class, String.class, String.class,
                        String.class, String.class, String.class};
            default:
                throw new IllegalArgumentException("未知方法: " + methodName);
        }
    }

    private AgreementDraftResult draftResult(String body, boolean adjusted) {
        return AgreementDraftResult.builder()
                .draftId("DRF-1")
                .agreementType("PRIVACY_POLICY")
                .scene("IVI_ACTIVATION")
                .market("DEFAULT")
                .locale("zh-CN")
                .brandCode("DEFAULT")
                .carLineCode("DEFAULT")
                .title("隐私政策")
                .version("1.0")
                .agreementDate(LocalDate.of(2026, 11, 20))
                .contentType("text/html; charset=utf-8")
                .body(body)
                .sanitizerVersion("html-allowlist-v1")
                .adjusted(adjusted)
                .lockVersion(0)
                .updatedBy("tester")
                .updatedAt(Instant.parse("2026-11-20T02:00:00Z"))
                .build();
    }

    private AgreementRevisionResult revisionResult() {
        return AgreementRevisionResult.builder()
                .revisionId("REV-1")
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
                .contentType("text/html; charset=utf-8")
                .body("<p>正文</p>")
                .contentHash("sha256:abc")
                .sanitizerVersion("html-allowlist-v1")
                .sourceDraftId("DRF-1")
                .sourceDraftVersion(2)
                .frozenBy("tester")
                .frozenAt(Instant.parse("2026-11-20T02:00:00Z"))
                .build();
    }
}
