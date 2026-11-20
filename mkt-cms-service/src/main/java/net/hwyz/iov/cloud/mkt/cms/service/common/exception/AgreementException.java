package net.hwyz.iov.cloud.mkt.cms.service.common.exception;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 协议内容子域业务异常。
 * <p>
 * 携带统一错误码与可读明细；明细不得包含协议正文（日志与响应都不打印正文）。
 *
 * @author hwyz_leo
 */
public class AgreementException extends BusinessException {

    public AgreementException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AgreementException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public AgreementException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public AgreementException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail);
        initCause(cause);
    }
}
