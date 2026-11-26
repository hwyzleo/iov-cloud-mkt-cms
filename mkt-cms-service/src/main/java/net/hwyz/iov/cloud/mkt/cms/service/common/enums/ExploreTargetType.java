package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

/**
 * 探索页跳转目标类型（判别联合）。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.2 / D-012：
 * ARTICLE 必须有 articleId + revisionId；TOPIC 必须有 topicId + revisionId；
 * H5 必须是白名单 origin 的绝对 HTTPS URL；NATIVE 必须使用元数据下发的 routeCode 与受控参数。
 *
 * @author hwyz_leo
 */
public enum ExploreTargetType {

    /**
     * 文章详情
     */
    ARTICLE,

    /**
     * 专题详情
     */
    TOPIC,

    /**
     * 白名单 H5 页面
     */
    H5,

    /**
     * 原生页面
     */
    NATIVE;

    /**
     * 解析目标类型
     *
     * @param value 目标类型字符串
     * @return 目标类型
     */
    public static ExploreTargetType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "跳转目标类型不能为空");
        }
        for (ExploreTargetType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "不支持的跳转目标类型: " + value);
    }
}
