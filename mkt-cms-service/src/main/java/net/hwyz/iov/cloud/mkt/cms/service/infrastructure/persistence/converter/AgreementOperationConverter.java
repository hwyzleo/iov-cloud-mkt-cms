package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementOperation;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementOperationPo;

/**
 * 操作留痕对象转换器（Domain ↔ PO）。
 *
 * @author hwyz_leo
 */
public final class AgreementOperationConverter {

    private AgreementOperationConverter() {
    }

    /**
     * 领域对象转持久化对象
     *
     * @param operation 领域对象
     * @return 持久化对象
     */
    public static AgreementOperationPo fromDomain(AgreementOperation operation) {
        AgreementOperationPo po = new AgreementOperationPo();
        po.setOperationId(operation.getOperationId());
        po.setAction(operation.getAction().name());
        po.setObjectType(operation.getObjectType());
        po.setObjectId(operation.getObjectId());
        po.setOperator(operation.getOperator());
        po.setOperatedAt(operation.getOperatedAt());
        po.setResult(operation.getResult());
        po.setTraceId(operation.getTraceId());
        return po;
    }
}
