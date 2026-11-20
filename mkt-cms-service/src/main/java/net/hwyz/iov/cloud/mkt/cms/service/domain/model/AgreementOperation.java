package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementAction;

import java.time.Instant;

/**
 * 协议操作留痕（成功操作与业务同事务；失败尝试走独立安全日志，不落本表）。
 * <p>
 * MKT-CMS-DSN-CR-001 §7：审计记录对象、操作人、时间、动作及结果，不记录协议正文、
 * 不记录无必要的 VIN / 账号数据。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementOperation {

    /**
     * 成功结果
     */
    public static final String RESULT_SUCCESS = "SUCCESS";

    private Long id;

    private String operationId;

    private AgreementAction action;

    private String objectType;

    private String objectId;

    private String operator;

    private Instant operatedAt;

    private String result;

    private String traceId;

    /**
     * 成功操作留痕
     *
     * @param operationId 操作标识
     * @param action      动作
     * @param objectType  对象类型
     * @param objectId    对象业务标识
     * @param operator    操作人
     * @param now         服务端时间
     * @param traceId     链路追踪 ID（可为 null）
     * @return 操作记录
     */
    public static AgreementOperation success(String operationId, AgreementAction action, String objectType,
                                             String objectId, String operator, Instant now, String traceId) {
        return AgreementOperation.builder()
                .operationId(operationId)
                .action(action)
                .objectType(objectType)
                .objectId(objectId)
                .operator(operator)
                .operatedAt(now)
                .result(RESULT_SUCCESS)
                .traceId(traceId)
                .build();
    }
}
