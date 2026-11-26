package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

/**
 * 探索页内容根实体状态。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.1：根实体状态为 PUBLISHED / OFFLINE；
 * OFFLINE 是手机端实时过滤条件，不受页面快照影响。
 *
 * @author hwyz_leo
 */
public enum ExploreContentStatus {

    /**
     * 已发布（至少有一个已发布修订）
     */
    PUBLISHED,

    /**
     * 已下线（手机端立即不可见，历史保留）
     */
    OFFLINE
}
