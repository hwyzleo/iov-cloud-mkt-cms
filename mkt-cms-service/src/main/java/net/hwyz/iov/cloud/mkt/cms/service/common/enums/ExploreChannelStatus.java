package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

/**
 * 探索页频道启停状态。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1 / AC-029：频道编码稳定，名称、排序与启停状态可维护。
 *
 * @author hwyz_leo
 */
public enum ExploreChannelStatus {

    /**
     * 启用：允许手机端读取
     */
    ENABLED,

    /**
     * 停用：手机端不返回该频道
     */
    DISABLED;

    /**
     * 解析频道状态（显式空值与未知值拒绝）
     *
     * @param value 状态字符串
     * @return 频道状态
     */
    public static ExploreChannelStatus parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "频道状态不能为空");
        }
        for (ExploreChannelStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的频道状态: " + value);
    }
}
