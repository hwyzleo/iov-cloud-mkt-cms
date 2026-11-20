package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.support;

import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;

/**
 * 操作人解析：身份只取安全上下文，不信任请求体自报操作人。
 *
 * @author hwyz_leo
 */
public final class OperatorResolver {

    private OperatorResolver() {
    }

    /**
     * 当前操作人（管理后台登录用户名）
     *
     * @return 操作人；安全上下文缺失时返回 null，由应用层拒绝
     */
    public static String currentOperator() {
        String username = SecurityUtils.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        Long userId = SecurityUtils.getUserId();
        return userId == null ? null : String.valueOf(userId);
    }
}
