package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreOperationMapper;
import org.springframework.stereotype.Repository;

/**
 * 探索页操作留痕仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ExploreOperationRepositoryImpl implements ExploreOperationRepository {

    private final ExploreOperationMapper operationMapper;

    @Override
    public void insert(ExploreOperation operation) {
        operationMapper.insert(ExploreConverter.fromDomain(operation));
    }
}
