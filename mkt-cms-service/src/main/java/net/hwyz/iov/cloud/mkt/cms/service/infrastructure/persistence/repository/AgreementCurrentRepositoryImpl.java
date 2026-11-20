package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementCurrentRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.AgreementBundleConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.AgreementCurrentMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 协议当前指针仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class AgreementCurrentRepositoryImpl implements AgreementCurrentRepository {

    private final AgreementCurrentMapper agreementCurrentMapper;

    @Override
    public Optional<AgreementCurrent> lockByScope(AgreementScope scope) {
        return Optional.ofNullable(agreementCurrentMapper.selectForUpdate(
                        scope.getScene().name(), scope.getMarket(), scope.getLocale(),
                        scope.getBrandCode(), scope.getCarLineCode()))
                .map(AgreementBundleConverter::toDomain);
    }

    @Override
    public void insertEmpty(AgreementScope scope, Instant now) {
        agreementCurrentMapper.insertPo(
                AgreementBundleConverter.fromDomain(AgreementCurrent.empty(scope, now)));
    }

    @Override
    public int switchPointer(AgreementScope scope, int expectedLockVersion, String newBundleId, Instant now) {
        return agreementCurrentMapper.switchPointer(scope.getScene().name(), scope.getMarket(), scope.getLocale(),
                scope.getBrandCode(), scope.getCarLineCode(), newBundleId, expectedLockVersion, now);
    }
}
