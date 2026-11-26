package net.hwyz.iov.cloud.mkt.cms.service.common.exception;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 探索页子域业务异常。
 * <p>
 * 携带统一错误码与可读明细；明细不得包含文章正文或敏感个人信息。
 *
 * @author hwyz_leo
 */
public class ExploreException extends BusinessException {

    public ExploreException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExploreException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public ExploreException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ExploreException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail);
        initCause(cause);
    }
}
