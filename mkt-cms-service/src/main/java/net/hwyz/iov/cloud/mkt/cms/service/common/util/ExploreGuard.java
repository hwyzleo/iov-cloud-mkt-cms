package net.hwyz.iov.cloud.mkt.cms.service.common.util;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import org.springframework.util.StringUtils;

/**
 * 探索页应用层通用校验。
 *
 * @author hwyz_leo
 */
public final class ExploreGuard {

    private ExploreGuard() {
    }

    /**
     * 校验并返回操作人
     *
     * @param operator 操作人
     * @return 去空白后的操作人
     */
    public static String requireOperator(String operator) {
        if (!StringUtils.hasText(operator)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "无法从安全上下文识别操作人");
        }
        return operator.trim();
    }

    /**
     * 校验必填文本与最大长度
     *
     * @param value     文本
     * @param field     字段名
     * @param maxLength 最大长度
     * @return 去空白后的文本
     */
    public static String requireText(String value, String field, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, field + " 不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    field + " 长度不得超过 " + maxLength);
        }
        return trimmed;
    }

    /**
     * 校验可选文本长度
     *
     * @param value     文本
     * @param field     字段名
     * @param maxLength 最大长度
     * @return 去空白后的文本（空值返回 null）
     */
    public static String optionalText(String value, String field, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    field + " 长度不得超过 " + maxLength);
        }
        return trimmed;
    }

    /**
     * 校验乐观锁版本
     *
     * @param lockVersion 版本
     * @return 版本
     */
    public static int requireLockVersion(Integer lockVersion) {
        if (lockVersion == null || lockVersion < 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "lockVersion 不能为空");
        }
        return lockVersion;
    }

    /**
     * 校验条目时间范围
     *
     * @param startAt 生效时间
     * @param endAt   失效时间
     */
    public static void validateTimeRange(java.time.Instant startAt, java.time.Instant endAt) {
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "失效时间必须晚于生效时间");
        }
    }
}
