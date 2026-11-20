package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.handler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 协议子域异常处理器。
 * <p>
 * MKT-CMS-DSN-CR-001 §7（D-005）：参数错误 400、无当前包/修订或草稿不存在 404、
 * 版本/发布/幂等冲突 409、数据源暂不可用 503；响应体仍是平台统一错误包装（含 traceId），
 * 不返回草稿、成功空包或部分协议正文。
 *
 * @author hwyz_leo
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AgreementExceptionHandler {

    /**
     * 协议子域业务异常
     *
     * @param exception 业务异常
     * @param response  HTTP 响应（写入 no-store，避免错误被缓存）
     * @return 统一错误包装
     */
    @ExceptionHandler(AgreementException.class)
    public ResponseEntity<ApiResponse<Void>> handleAgreementException(AgreementException exception,
                                                                     HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("协议子域业务异常: code={}, message={}", errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(statusOf(errorCode))
                .body(ApiResponse.fail(errorCode, exception.getMessage()));
    }

    /**
     * 请求体字段校验失败
     *
     * @param exception 校验异常
     * @return 400 + 参数校验错误码
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(BindException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("请求参数校验失败");
        return badRequest(detail);
    }

    /**
     * 缺少必填请求参数
     *
     * @param exception 缺参异常
     * @return 400 + 参数校验错误码
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return badRequest("缺少必填参数: " + exception.getParameterName());
    }

    /**
     * 参数类型不匹配
     *
     * @param exception 类型异常
     * @return 400 + 参数校验错误码
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest("参数类型不合法: " + exception.getName());
    }

    /**
     * 请求体不可解析
     *
     * @param exception 解析异常
     * @return 400 + 参数校验错误码
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException exception) {
        log.warn("协议请求体解析失败: {}", exception.getMessage());
        return badRequest("请求体格式不合法");
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String detail) {
        log.warn("协议请求参数错误: {}", detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, detail));
    }

    private HttpStatus statusOf(ErrorCode errorCode) {
        String code = errorCode.getCode();
        if (AgreementErrorCode.AGREEMENT_CURRENT_NOT_FOUND.getCode().equals(code)
                || AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND.getCode().equals(code)
                || AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND.getCode().equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (AgreementErrorCode.AGREEMENT_VERSION_CONFLICT.getCode().equals(code)
                || AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT.getCode().equals(code)
                || AgreementErrorCode.AGREEMENT_IDEMPOTENCY_CONFLICT.getCode().equals(code)) {
            return HttpStatus.CONFLICT;
        }
        if (AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE.getCode().equals(code)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
