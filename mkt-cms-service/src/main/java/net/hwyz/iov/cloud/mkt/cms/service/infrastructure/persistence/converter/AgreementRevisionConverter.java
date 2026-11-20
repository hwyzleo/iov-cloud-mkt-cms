package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementRevisionPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.CurrentBundleViewPo;

/**
 * 修订对象转换器（Domain ↔ PO）。
 *
 * @author hwyz_leo
 */
public final class AgreementRevisionConverter {

    private AgreementRevisionConverter() {
    }

    /**
     * 持久化对象转领域对象
     *
     * @param po 持久化对象
     * @return 领域对象；入参为空时返回 null
     */
    public static AgreementRevision toDomain(AgreementRevisionPo po) {
        if (po == null) {
            return null;
        }
        return AgreementRevision.builder()
                .id(po.getId())
                .revisionId(po.getRevisionId())
                .agreementCode(po.getAgreementCode())
                .type(AgreementType.parse(po.getAgreementType()))
                .scope(AgreementScope.of(AgreementScene.parse(po.getScene()), po.getMarket(), po.getLocale(),
                        po.getBrandCode(), po.getCarLineCode()))
                .version(po.getVersion())
                .agreementDate(po.getAgreementDate())
                .title(po.getTitle())
                .contentType(po.getContentType())
                .body(po.getBody())
                .contentHash(po.getContentHash())
                .sanitizerVersion(po.getSanitizerVersion())
                .sourceDraftId(po.getSourceDraftId())
                .sourceDraftVersion(po.getSourceDraftVersion() == null ? 0 : po.getSourceDraftVersion())
                .frozenBy(po.getFrozenBy())
                .frozenAt(po.getFrozenAt())
                .build();
    }

    /**
     * 领域对象转持久化对象
     *
     * @param revision 领域对象
     * @return 持久化对象
     */
    public static AgreementRevisionPo fromDomain(AgreementRevision revision) {
        AgreementRevisionPo po = new AgreementRevisionPo();
        po.setRevisionId(revision.getRevisionId());
        po.setAgreementCode(revision.getAgreementCode());
        po.setAgreementType(revision.getType().name());
        po.setScene(revision.getScope().getScene().name());
        po.setMarket(revision.getScope().getMarket());
        po.setLocale(revision.getScope().getLocale());
        po.setBrandCode(revision.getScope().getBrandCode());
        po.setCarLineCode(revision.getScope().getCarLineCode());
        po.setTitle(revision.getTitle());
        po.setVersion(revision.getVersion());
        po.setAgreementDate(revision.getAgreementDate());
        po.setContentType(revision.getContentType());
        po.setBody(revision.getBody());
        po.setContentHash(revision.getContentHash());
        po.setSanitizerVersion(revision.getSanitizerVersion());
        po.setSourceDraftId(revision.getSourceDraftId());
        po.setSourceDraftVersion(revision.getSourceDraftVersion());
        po.setFrozenBy(revision.getFrozenBy());
        po.setFrozenAt(revision.getFrozenAt());
        return po;
    }

    /**
     * 当前包一致性读取结果的隐私政策侧转领域对象
     *
     * @param view 一次性读取结果
     * @return 隐私政策修订
     */
    public static AgreementRevision privacyOf(CurrentBundleViewPo view) {
        return AgreementRevision.builder()
                .revisionId(view.getPRevisionId())
                .agreementCode(view.getPAgreementCode())
                .type(AgreementType.parse(view.getPAgreementType()))
                .scope(AgreementScope.of(AgreementScene.parse(view.getPScene()), view.getPMarket(),
                        view.getPLocale(), view.getPBrandCode(), view.getPCarLineCode()))
                .title(view.getPTitle())
                .version(view.getPVersion())
                .agreementDate(view.getPAgreementDate())
                .contentType(view.getPContentType())
                .body(view.getPBody())
                .contentHash(view.getPContentHash())
                .sanitizerVersion(view.getPSanitizerVersion())
                .build();
    }

    /**
     * 当前包一致性读取结果的用户协议侧转领域对象
     *
     * @param view 一次性读取结果
     * @return 用户协议修订
     */
    public static AgreementRevision userAgreementOf(CurrentBundleViewPo view) {
        return AgreementRevision.builder()
                .revisionId(view.getURevisionId())
                .agreementCode(view.getUAgreementCode())
                .type(AgreementType.parse(view.getUAgreementType()))
                .scope(AgreementScope.of(AgreementScene.parse(view.getUScene()), view.getUMarket(),
                        view.getULocale(), view.getUBrandCode(), view.getUCarLineCode()))
                .title(view.getUTitle())
                .version(view.getUVersion())
                .agreementDate(view.getUAgreementDate())
                .contentType(view.getUContentType())
                .body(view.getUBody())
                .contentHash(view.getUContentHash())
                .sanitizerVersion(view.getUSanitizerVersion())
                .build();
    }
}
