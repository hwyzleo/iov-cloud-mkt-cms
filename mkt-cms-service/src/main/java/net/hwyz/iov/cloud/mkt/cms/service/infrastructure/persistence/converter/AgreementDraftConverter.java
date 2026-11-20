package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementDraftPo;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;

/**
 * 草稿对象转换器（Domain ↔ PO）。
 *
 * @author hwyz_leo
 */
public final class AgreementDraftConverter {

    private AgreementDraftConverter() {
    }

    /**
     * 持久化对象转领域对象
     *
     * @param po 持久化对象
     * @return 领域对象；入参为空时返回 null
     */
    public static AgreementDraft toDomain(AgreementDraftPo po) {
        if (po == null) {
            return null;
        }
        return AgreementDraft.builder()
                .draftId(po.getDraftId())
                .type(AgreementType.parse(po.getAgreementType()))
                .scope(AgreementScope.of(AgreementScene.parse(po.getScene()), po.getMarket(), po.getLocale(),
                        po.getBrandCode(), po.getCarLineCode()))
                .title(po.getTitle())
                .version(po.getVersion())
                .agreementDate(po.getAgreementDate())
                .contentType(po.getContentType())
                .body(po.getBody())
                .sanitizerVersion(po.getSanitizerVersion())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .createdBy(po.getCreateBy())
                .createdAt(po.getCreateTime())
                .updatedBy(po.getModifyBy())
                .updatedAt(po.getModifyTime())
                .build();
    }

    /**
     * 领域对象转持久化对象
     *
     * @param draft 领域对象
     * @return 持久化对象
     */
    public static AgreementDraftPo fromDomain(AgreementDraft draft) {
        AgreementDraftPo po = new AgreementDraftPo();
        po.setDraftId(draft.getDraftId());
        po.setAgreementType(draft.getType().name());
        po.setScene(draft.getScope().getScene().name());
        po.setMarket(draft.getScope().getMarket());
        po.setLocale(draft.getScope().getLocale());
        po.setBrandCode(draft.getScope().getBrandCode());
        po.setCarLineCode(draft.getScope().getCarLineCode());
        po.setTitle(draft.getTitle());
        po.setVersion(draft.getVersion());
        po.setAgreementDate(draft.getAgreementDate());
        po.setContentType(draft.getContentType());
        po.setBody(draft.getBody());
        po.setSanitizerVersion(draft.getSanitizerVersion());
        po.setLockVersion(draft.getLockVersion());
        po.setCreateBy(draft.getCreatedBy());
        po.setCreateTime(draft.getCreatedAt());
        po.setModifyBy(draft.getUpdatedBy());
        po.setModifyTime(draft.getUpdatedAt());
        return po;
    }
}
