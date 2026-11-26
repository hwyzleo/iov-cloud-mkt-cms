package net.hwyz.iov.cloud.mkt.cms.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * MKT-CMS 探索页子域错误码（CMS 105000 错误码段）。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.3 / D-017：数字码 105009～105017 与语义一一对应；
 * 认证/授权失败复用平台统一认证错误码，不在本枚举占号。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum ExploreErrorCode implements ErrorCode {

    /**
     * 参数、组件、富文本、素材、目标、游标或时间范围非法
     */
    EXPLORE_INVALID_ARGUMENT("105009", "探索页参数校验失败"),

    /**
     * 内容不存在，或手机请求未发布/已下线内容
     */
    EXPLORE_CONTENT_NOT_FOUND("105010", "探索页内容不存在或不可读"),

    /**
     * 频道无当前有效页面
     */
    EXPLORE_PAGE_NOT_FOUND("105011", "探索页频道当前无有效页面"),

    /**
     * 内容或页面草稿乐观锁冲突
     */
    EXPLORE_VERSION_CONFLICT("105012", "探索页版本冲突"),

    /**
     * expectedReleaseId 已过期或发布前提变化
     */
    EXPLORE_PUBLISH_CONFLICT("105013", "探索页发布前提失效"),

    /**
     * requestId 对应不同命令
     */
    EXPLORE_IDEMPOTENCY_CONFLICT("105014", "探索页幂等标识对应不同命令"),

    /**
     * 已存在待生效版本或调度状态不允许操作
     */
    EXPLORE_SCHEDULE_CONFLICT("105015", "探索页调度冲突"),

    /**
     * 数据源异常或调度重试耗尽，不返回部分页面
     */
    EXPLORE_TEMPORARILY_UNAVAILABLE("105016", "探索页服务暂不可用"),

    /**
     * 翻页 releaseId 已不是当前版本，客户端须刷新首页
     */
    EXPLORE_RELEASE_CHANGED("105017", "探索页发布版本已变化");

    private final String code;
    private final String message;
}
