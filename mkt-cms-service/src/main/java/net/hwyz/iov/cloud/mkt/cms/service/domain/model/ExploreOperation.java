package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;

import java.time.Instant;

/**
 * 探索页操作留痕（成功操作与业务同事务）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreOperation {

    private Long id;

    private String operationId;

    private String objectType;

    private String objectId;

    private ExploreAction action;

    private String operator;

    private Instant operatedAt;

    private String result;

    private String requestId;

    private String traceId;

    private String detail;

    /**
     * 成功操作记录
     *
     * @param operationId 操作标识
     * @param action      动作
     * @param objectType  对象类型
     * @param objectId    对象标识
     * @param operator    操作人
     * @param operatedAt  操作时间
     * @param requestId   幂等请求标识（可空）
     * @param traceId     链路追踪标识（可空）
     * @param detail      受控摘要（不得包含正文）
     * @return 操作记录
     */
    public static ExploreOperation success(String operationId, ExploreAction action, String objectType,
                                           String objectId, String operator, Instant operatedAt,
                                           String requestId, String traceId, String detail) {
        return ExploreOperation.builder()
                .operationId(operationId)
                .objectType(objectType)
                .objectId(objectId)
                .action(action)
                .operator(operator)
                .operatedAt(operatedAt)
                .result("SUCCESS")
                .requestId(requestId)
                .traceId(traceId)
                .detail(detail)
                .build();
    }
}
