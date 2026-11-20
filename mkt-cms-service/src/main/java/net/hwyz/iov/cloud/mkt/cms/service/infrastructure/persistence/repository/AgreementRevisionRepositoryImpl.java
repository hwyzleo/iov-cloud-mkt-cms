package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementRevisionRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.AgreementRevisionConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.AgreementRevisionMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 协议修订仓储实现（只增只读）。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class AgreementRevisionRepositoryImpl implements AgreementRevisionRepository {

    private final AgreementRevisionMapper agreementRevisionMapper;

    @Override
    public AgreementRevision insert(AgreementRevision revision) {
        agreementRevisionMapper.insertPo(AgreementRevisionConverter.fromDomain(revision));
        return revision;
    }

    @Override
    public Optional<AgreementRevision> findByRevisionId(String revisionId) {
        return Optional.ofNullable(agreementRevisionMapper.selectByRevisionId(revisionId))
                .map(AgreementRevisionConverter::toDomain);
    }

    @Override
    public Optional<AgreementRevision> findBySourceDraft(String sourceDraftId, int sourceDraftVersion) {
        return Optional.ofNullable(agreementRevisionMapper.selectBySourceDraft(sourceDraftId, sourceDraftVersion))
                .map(AgreementRevisionConverter::toDomain);
    }

    @Override
    public Optional<AgreementRevision> findPublishedByRevisionId(String revisionId) {
        return Optional.ofNullable(agreementRevisionMapper.selectPublishedByRevisionId(revisionId))
                .map(AgreementRevisionConverter::toDomain);
    }
}
