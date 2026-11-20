package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.AgreementOperationConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.AgreementCurrentMapper;
import org.springframework.stereotype.Repository;

/**
 * 协议操作留痕仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class AgreementOperationRepositoryImpl implements AgreementOperationRepository {

    private final AgreementCurrentMapper agreementCurrentMapper;

    @Override
    public void insert(AgreementOperation operation) {
        agreementCurrentMapper.insertOperationPo(AgreementOperationConverter.fromDomain(operation));
    }
}
