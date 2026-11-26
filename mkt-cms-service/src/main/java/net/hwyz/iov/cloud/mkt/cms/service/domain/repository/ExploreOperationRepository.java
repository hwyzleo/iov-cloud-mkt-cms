package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;

/**
 * 探索页操作留痕仓储。
 *
 * @author hwyz_leo
 */
public interface ExploreOperationRepository {

    /**
     * 写入成功操作记录（与业务同事务）
     *
     * @param operation 操作记录
     */
    void insert(ExploreOperation operation);
}
