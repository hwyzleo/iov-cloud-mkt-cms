package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementDraftRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.AgreementDraftConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.AgreementDraftMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementDraftPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
}
