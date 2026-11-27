package net.hwyz.iov.cloud.mkt.cms.service.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * 探索页业务标识生成器。
 * <p>
 * 标识由服务端生成，不使用客户端提交值作为权威标识；前缀与不可变对象类型对应，便于排障与审计。
 *
 * @author hwyz_leo
 */
public final class ExploreIdGenerator {

    private static final int RANDOM_LENGTH = 20;

    private ExploreIdGenerator() {
    }

    /**
     * 文章根实体标识（ea_）
     */
    public static String articleId() {
        return "ea_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 文章修订标识（ear_）
     */
    public static String articleRevisionId() {
        return "ear_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 专题根实体标识（et_）
     */
    public static String topicId() {
        return "et_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 专题修订标识（etr_）
     */
    public static String topicRevisionId() {
        return "etr_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * Banner 根实体标识（eb_）
     */
    public static String bannerId() {
        return "eb_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * Banner 修订标识（ebr_）
     */
    public static String bannerRevisionId() {
        return "ebr_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 页面标识（ep_）
     */
    public static String pageId() {
        return "ep_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 发布版本标识（er_）
     */
    public static String releaseId() {
        return "er_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 页面组件标识（ecp_）
     */
    public static String componentId() {
        return "ecp_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 页面条目标识（epi_）
     */
    public static String itemId() {
        return "epi_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 操作留痕标识（eop_）
     */
    public static String operationId() {
        return "eop_" + IdUtil.nanoId(RANDOM_LENGTH);
    }

    /**
     * 频道配置修订标识（ecr_）
     */
    public static String channelRevisionId() {
        return "ecr_" + IdUtil.nanoId(RANDOM_LENGTH);
    }
}
