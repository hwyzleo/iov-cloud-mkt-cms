package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

/**
 * 探索页文章来源类型。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1 / AC-033：官方文章使用安全富文本；
 * UGC_REFERENCE 仅保存已发布内容引用与展示快照，不提供发帖、评论、点赞或审核工作流。
 *
 * @author hwyz_leo
 */
public enum ExploreSourceType {

    /**
     * 官方文章
     */
    OFFICIAL,

    /**
     * 已发布用户内容引用
     */
    UGC_REFERENCE;

    /**
     * 解析来源类型
     *
     * @param value 来源类型字符串
     * @return 来源类型
     */
    public static ExploreSourceType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "文章来源类型不能为空");
        }
        for (ExploreSourceType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的文章来源类型: " + value);
    }
}
