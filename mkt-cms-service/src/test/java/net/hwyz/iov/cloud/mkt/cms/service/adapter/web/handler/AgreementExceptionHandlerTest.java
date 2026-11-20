package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler;

import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 协议子域异常映射测试（MKT-CMS-DSN-CR-001 §7 / AT-007）。
 *
 * @author hwyz_leo
 */
@DisplayName("协议错误语义 - 400/404/409/503 映射")
class AgreementExceptionHandlerTest {

    private final AgreementExceptionHandler handler = new AgreementExceptionHandler();

    @Test
    @DisplayName("参数/前提错误映射 400")
    void 参数错误映射() {
        assertStatus(HttpStatus.BAD_REQUEST, AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT);
    }

    @Test
    @DisplayName("无当前包/修订或草稿不存在映射 404")
    void 不存在映射() {
        assertStatus(HttpStatus.NOT_FOUND, AgreementErrorCode.AGREEMENT_CURRENT_NOT_FOUND);
        assertStatus(HttpStatus.NOT_FOUND, AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND);
        assertStatus(HttpStatus.NOT_FOUND, AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND);
    }

    @Test
    @DisplayName("版本/发布/幂等冲突映射 409")
    void 冲突映射() {
        assertStatus(HttpStatus.CONFLICT, AgreementErrorCode.AGREEMENT_VERSION_CONFLICT);
        assertStatus(HttpStatus.CONFLICT, AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT);
        assertStatus(HttpStatus.CONFLICT, AgreementErrorCode.AGREEMENT_IDEMPOTENCY_CONFLICT);
    }

    @Test
    @DisplayName("暂不可用映射 503")
    void 暂不可用映射() {
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE);
    }

    @Test
    @DisplayName("错误响应保持统一包装与 traceId 字段，且不缓存")
    void 错误包装() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ResponseEntity<ApiResponse<Void>> entity = handler.handleAgreementException(
                new AgreementException(AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT, "当前发布包已变化"), response);

        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        ApiResponse<Void> body = entity.getBody();
        assertEquals("105006", body.getCode());
        assertEquals("当前发布包已变化", body.getMessage());
        assertEquals(null, body.getData());
    }

    private void assertStatus(HttpStatus expected, AgreementErrorCode errorCode) {
        ResponseEntity<ApiResponse<Void>> entity = handler.handleAgreementException(
                new AgreementException(errorCode), new MockHttpServletResponse());
        assertEquals(expected, entity.getStatusCode());
        assertEquals(errorCode.getCode(), entity.getBody().getCode());
    }
}
