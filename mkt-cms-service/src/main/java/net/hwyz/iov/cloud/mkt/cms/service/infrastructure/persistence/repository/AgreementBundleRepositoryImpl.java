package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.CurrentBundleView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementBundleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.AgreementBundleConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.AgreementBundleMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 协议发布包仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class AgreementBundleRepositoryImpl implements AgreementBundleRepository {

    private final AgreementBundleMapper agreementBundleMapper;

    @Override
    public AgreementBundle insert(AgreementBundle bundle) {
        agreementBundleMapper.insertPo(AgreementBundleConverter.fromDomain(bundle));
        return bundle;
    }

    @Override
    public Optional<AgreementBundle> findByBundleId(String bundleId) {
        return Optional.ofNullable(agreementBundleMapper.selectByBundleId(bundleId))
                .map(AgreementBundleConverter::toDomain);
    }

    @Override
    public Optional<AgreementBundle> findByRequestId(String requestId) {
        return Optional.ofNullable(agreementBundleMapper.selectByRequestId(requestId))
                .map(AgreementBundleConverter::toDomain);
    }

    @Override
    public Optional<AgreementBundle> findByRequestIdForUpdate(String requestId) {
        return Optional.ofNullable(agreementBundleMapper.selectByRequestIdForUpdate(requestId))
                .map(AgreementBundleConverter::toDomain);
    }

    @Override
    public Optional<CurrentBundleView> findCurrentView(AgreementScope scope) {
        return Optional.ofNullable(agreementBundleMapper.selectCurrentView(
                        scope.getScene().name(), scope.getMarket(), scope.getLocale(),
                        scope.getBrandCode(), scope.getCarLineCode()))
                .map(AgreementBundleConverter::toCurrentView);
    }
}
