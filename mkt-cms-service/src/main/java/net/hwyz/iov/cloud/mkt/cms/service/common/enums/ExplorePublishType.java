package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

/**
 * 探索页页面发布类型。
 *
 * @author hwyz_leo
 */
public enum ExplorePublishType {

    /**
     * 立即发布
     */
    IMMEDIATE,

    /**
     * 定时发布
     */
    SCHEDULED;

    /**
     * 解析发布类型
     *
     * @param value 发布类型字符串
     * @return 发布类型
     */
    public static ExplorePublishType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "发布类型不能为空");
        }
        for (ExplorePublishType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的发布类型: " + value);
    }
}
