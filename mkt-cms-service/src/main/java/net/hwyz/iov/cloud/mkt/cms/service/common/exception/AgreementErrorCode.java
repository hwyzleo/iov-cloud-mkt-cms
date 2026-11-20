package net.hwyz.iov.cloud.mkt.cms.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * MKT-CMS 协议内容子域错误码（CMS 105000 错误码段）。
 * <p>
 * MKT-CMS-DSN-CR-001 §7（D-005）：语义与数字码一一对应；
 * 认证/授权失败复用平台统一认证错误码，不在本枚举占号。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum AgreementErrorCode implements ErrorCode {

    /**
     * 必填、枚举、UTF-8、大小、范围或包结构错误
     */
    AGREEMENT_INVALID_ARGUMENT("105001", "协议参数校验失败"),

    /**
     * 范围合法但没有当前发布包
     */
    AGREEMENT_CURRENT_NOT_FOUND("105002", "当前协议发布包不存在"),

    /**
     * 修订不存在，或业务读取请求指向未发布修订
     */
    AGREEMENT_REVISION_NOT_FOUND("105003", "协议修订不存在或未发布"),

    /**
     * 管理操作所需草稿不存在
     */
    AGREEMENT_DRAFT_NOT_FOUND("105004", "协议草稿不存在"),

    /**
     * 身份下版本重复或草稿乐观锁冲突
     */
    AGREEMENT_VERSION_CONFLICT("105005", "协议版本冲突"),

    /**
     * expectedBundleId 已过期
     */
    AGREEMENT_PUBLISH_CONFLICT("105006", "当前发布包已变化，发布前提失效"),

    /**
     * requestId 对应不同命令
     */
    AGREEMENT_IDEMPOTENCY_CONFLICT("105007", "幂等标识对应不同命令"),

    /**
     * 数据源暂不可用或发布内容完整性异常，不能返回半套内容
     */
    AGREEMENT_TEMPORARILY_UNAVAILABLE("105008", "协议内容服务暂不可用");

    private final String code;
    private final String message;
}
