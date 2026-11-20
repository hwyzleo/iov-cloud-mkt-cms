package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementRevisionRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.AgreementRevisionConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.AgreementRevisionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public List<AgreementRevision> search(AgreementSearchCriteria criteria) {
        List<net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementRevisionPo> poList =
                agreementRevisionMapper.selectPoByCriteria(criteria);
        List<AgreementRevision> revisions = poList.stream()
                .map(AgreementRevisionConverter::toDomain)
                .collect(Collectors.toList());
        if (poList instanceof com.github.pagehelper.Page<
                net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementRevisionPo> page) {
            com.github.pagehelper.Page<AgreementRevision> paged =
                    new com.github.pagehelper.Page<>(page.getPageNum(), page.getPageSize(), page.isCount());
            paged.setTotal(page.getTotal());
            paged.addAll(revisions);
            return paged;
        }
        return revisions;
    }
}
