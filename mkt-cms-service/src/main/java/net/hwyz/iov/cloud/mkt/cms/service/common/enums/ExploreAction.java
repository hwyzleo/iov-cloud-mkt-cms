package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

/**
 * 探索页操作留痕动作。
 *
 * @author hwyz_leo
 */
public enum ExploreAction {

    /**
     * 新建内容草稿
     */
    CREATE,

    /**
     * 修改内容草稿
     */
    UPDATE,

    /**
     * 发布内容修订
     */
    PUBLISH,

    /**
     * 内容下线
     */
    OFFLINE,

    /**
     * 保存页面草稿
     */
    SAVE_DRAFT,

    /**
     * 发布页面版本（立即或定时）
     */
    PUBLISH_PAGE,

    /**
     * 取消定时发布
     */
    CANCEL_SCHEDULE,

    /**
     * 页面立即下线
     */
    OFFLINE_PAGE,

    /**
     * 调度器激活定时版本
     */
    SCHEDULE_ACTIVATE,

    /**
     * 调度器到期清除当前版本
     */
    SCHEDULE_EXPIRE,

    /**
     * 历史版本复制为新草稿
     */
    COPY_TO_DRAFT,

    /**
     * 修改频道草稿
     */
    UPDATE_CHANNEL,

    /**
     * 新增频道
     */
    CREATE_CHANNEL,

    /**
     * 删除未被引用的频道
     */
    DELETE_CHANNEL,

    /**
     * 独立发布频道配置（不影响页面发布）
     */
    PUBLISH_CHANNEL
}
