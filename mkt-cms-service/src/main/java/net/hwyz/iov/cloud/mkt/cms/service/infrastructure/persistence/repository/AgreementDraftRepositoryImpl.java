package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementDraftRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.AgreementDraftConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.AgreementDraftMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementDraftPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 协议草稿仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class AgreementDraftRepositoryImpl implements AgreementDraftRepository {

    private final AgreementDraftMapper agreementDraftMapper;

    @Override
    public AgreementDraft insert(AgreementDraft draft) {
        AgreementDraftPo po = AgreementDraftConverter.fromDomain(draft);
        agreementDraftMapper.insertPo(po);
        return draft;
    }

    @Override
    public int updateContent(AgreementDraft draft, int expectedLockVersion) {
        return agreementDraftMapper.updateContentWithLock(AgreementDraftConverter.fromDomain(draft), expectedLockVersion);
    }

    @Override
    public Optional<AgreementDraft> findByDraftId(String draftId) {
        return Optional.ofNullable(agreementDraftMapper.selectByDraftId(draftId))
                .map(AgreementDraftConverter::toDomain);
    }

    @Override
    public List<AgreementDraft> search(AgreementSearchCriteria criteria) {
        List<AgreementDraftPo> poList = agreementDraftMapper.selectPoByCriteria(criteria);
        List<AgreementDraft> drafts = poList.stream()
                .map(AgreementDraftConverter::toDomain)
                .collect(Collectors.toList());
        if (poList instanceof com.github.pagehelper.Page<AgreementDraftPo> page) {
            com.github.pagehelper.Page<AgreementDraft> paged =
                    new com.github.pagehelper.Page<>(page.getPageNum(), page.getPageSize(), page.isCount());
            paged.setTotal(page.getTotal());
            paged.addAll(drafts);
            return paged;
        }
        return drafts;
    }
}
