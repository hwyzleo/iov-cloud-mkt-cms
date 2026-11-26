package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

/**
 * 探索页组件类型。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1 / D-011：首版固定三类组件，每类组件每页最多一个，使用显式排序；
 * 不开放通用低代码 Schema 或可执行自定义组件。
 *
 * @author hwyz_leo
 */
public enum ExploreComponentType {

    /**
     * Banner 轮播
     */
    BANNER_CAROUSEL,

    /**
     * 专题网格（四列）
     */
    TOPIC_GRID,

    /**
     * 文章信息流
     */
    ARTICLE_FEED;

    /**
     * 解析组件类型
     *
     * @param value 组件类型字符串
     * @return 组件类型
     */
    public static ExploreComponentType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "组件类型不能为空");
        }
        for (ExploreComponentType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的组件类型: " + value);
    }
}
