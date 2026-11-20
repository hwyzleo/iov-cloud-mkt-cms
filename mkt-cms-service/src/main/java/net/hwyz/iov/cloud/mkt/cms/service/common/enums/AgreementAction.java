package net.hwyz.iov.cloud.mkt.cms.service.common.enums;

/**
 * 协议管理动作（用于操作留痕）。
 *
 * @author hwyz_leo
 */
public enum AgreementAction {

    /**
     * 新建草稿
     */
    CREATE_DRAFT,

    /**
     * 修改草稿
     */
    UPDATE_DRAFT,

    /**
     * 定稿（生成不可变修订）
     */
    FREEZE,

    /**
     * 成套发布（切换当前指针）
     */
    PUBLISH
}
