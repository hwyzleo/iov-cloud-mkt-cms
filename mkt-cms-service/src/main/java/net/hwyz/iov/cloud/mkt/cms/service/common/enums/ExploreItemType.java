package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

/**
 * 探索页页面条目引用的内容类型。
 *
 * @author hwyz_leo
 */
public enum ExploreItemType {

    /**
     * 引用 Banner 修订
     */
    BANNER,

    /**
     * 引用专题修订
     */
    TOPIC,

    /**
     * 引用文章修订
     */
    ARTICLE;

    /**
     * 解析条目类型
     *
     * @param value 条目类型字符串
     * @return 条目类型
     */
    public static ExploreItemType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "页面条目类型不能为空");
        }
        for (ExploreItemType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的页面条目类型: " + value);
    }
}
