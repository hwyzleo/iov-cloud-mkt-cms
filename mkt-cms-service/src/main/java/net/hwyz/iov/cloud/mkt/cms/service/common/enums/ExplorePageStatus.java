package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

/**
 * 探索页频道页面版本状态。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.2 / D-013：版本 0 为唯一草稿；发布版本为不可变快照，
 * 可处于待生效（SCHEDULED）、已生效（PUBLISHED）、已到期（EXPIRED）、已下线（OFFLINE）或已取消（CANCELED）。
 *
 * @author hwyz_leo
 */
public enum ExplorePageStatus {

    /**
     * 草稿（page_version=0，唯一）
     */
    DRAFT,

    /**
     * 待生效（定时发布）
     */
    SCHEDULED,

    /**
     * 已生效（当前版本或历史版本）
     */
    PUBLISHED,

    /**
     * 已到期（不自动回退旧版本）
     */
    EXPIRED,

    /**
     * 已立即下线
     */
    OFFLINE,

    /**
     * 定时发布已取消
     */
    CANCELED
}
