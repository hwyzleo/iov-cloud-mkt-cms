package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementOperation;

/**
 * 协议操作留痕仓储（域层接口，基础设施层实现）。
 *
 * @author hwyz_leo
 */
public interface AgreementOperationRepository {

    /**
     * 记录成功操作（与业务同事务）
     *
     * @param operation 操作记录
     */
    void insert(AgreementOperation operation);
}
