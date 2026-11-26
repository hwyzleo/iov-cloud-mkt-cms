package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

/**
 * 探索页内容修订状态。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1：修订状态为 DRAFT / PUBLISHED；发布后不可改删。
 *
 * @author hwyz_leo
 */
public enum ExploreRevisionStatus {

    /**
     * 草稿修订（可被发布）
     */
    DRAFT,

    /**
     * 已发布修订（不可变）
     */
    PUBLISHED;

    /**
     * 解析修订状态
     *
     * @param value 状态字符串
     * @return 修订状态
     */
    public static ExploreRevisionStatus parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "修订状态不能为空");
        }
        for (ExploreRevisionStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的修订状态: " + value);
    }
}
