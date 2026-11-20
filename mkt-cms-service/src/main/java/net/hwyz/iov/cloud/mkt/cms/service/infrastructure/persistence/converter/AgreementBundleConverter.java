package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.CurrentBundleView;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementBundlePo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementCurrentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.CurrentBundleViewPo;

/**
 * 发布包与当前指针对象转换器（Domain ↔ PO）。
 *
 * @author hwyz_leo
 */
public final class AgreementBundleConverter {

    private AgreementBundleConverter() {
    }

    /**
     * 发布包持久化对象转领域对象
     *
     * @param po 持久化对象
     * @return 领域对象；入参为空时返回 null
     */
    public static AgreementBundle toDomain(AgreementBundlePo po) {
        if (po == null) {
            return null;
        }
        return AgreementBundle.builder()
                .id(po.getId())
                .bundleId(po.getBundleId())
                .scope(AgreementScope.of(AgreementScene.parse(po.getScene()), po.getMarket(), po.getLocale(),
                        po.getBrandCode(), po.getCarLineCode()))
                .privacyRevisionId(po.getPrivacyRevisionId())
                .userRevisionId(po.getUserRevisionId())
                .publishedBy(po.getPublishedBy())
                .publishedAt(po.getPublishedAt())
                .requestId(po.getRequestId())
                .requestHash(po.getRequestHash())
                .expectedBundleId(po.getExpectedBundleId())
                .build();
    }

    /**
     * 发布包领域对象转持久化对象
     *
     * @param bundle 领域对象
     * @return 持久化对象
     */
    public static AgreementBundlePo fromDomain(AgreementBundle bundle) {
        AgreementBundlePo po = new AgreementBundlePo();
        po.setBundleId(bundle.getBundleId());
        po.setScene(bundle.getScope().getScene().name());
        po.setMarket(bundle.getScope().getMarket());
        po.setLocale(bundle.getScope().getLocale());
        po.setBrandCode(bundle.getScope().getBrandCode());
        po.setCarLineCode(bundle.getScope().getCarLineCode());
        po.setPrivacyRevisionId(bundle.getPrivacyRevisionId());
        po.setUserRevisionId(bundle.getUserRevisionId());
        po.setPublishedBy(bundle.getPublishedBy());
        po.setPublishedAt(bundle.getPublishedAt());
        po.setRequestId(bundle.getRequestId());
        po.setRequestHash(bundle.getRequestHash());
        po.setExpectedBundleId(bundle.getExpectedBundleId());
        return po;
    }

    /**
     * 当前指针持久化对象转领域对象
     *
     * @param po 持久化对象
     * @return 领域对象；入参为空时返回 null
     */
    public static AgreementCurrent toDomain(AgreementCurrentPo po) {
        if (po == null) {
            return null;
        }
        return AgreementCurrent.builder()
                .scope(AgreementScope.of(AgreementScene.parse(po.getScene()), po.getMarket(), po.getLocale(),
                        po.getBrandCode(), po.getCarLineCode()))
                .bundleId(po.getBundleId())
                .lockVersion(po.getLockVersion() == null ? 0 : po.getLockVersion())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    /**
     * 当前指针领域对象转持久化对象
     *
     * @param current 领域对象
     * @return 持久化对象
     */
    public static AgreementCurrentPo fromDomain(AgreementCurrent current) {
        AgreementCurrentPo po = new AgreementCurrentPo();
        po.setScene(current.getScope().getScene().name());
        po.setMarket(current.getScope().getMarket());
        po.setLocale(current.getScope().getLocale());
        po.setBrandCode(current.getScope().getBrandCode());
        po.setCarLineCode(current.getScope().getCarLineCode());
        po.setBundleId(current.getBundleId());
        po.setLockVersion(current.getLockVersion());
        po.setUpdatedAt(current.getUpdatedAt());
        return po;
    }

    /**
     * 一次性读取结果转当前发布包视图
     *
     * @param po 一次性读取结果
     * @return 当前发布包视图；入参为空时返回 null
     */
    public static CurrentBundleView toCurrentView(CurrentBundleViewPo po) {
        if (po == null) {
            return null;
        }
        return CurrentBundleView.builder()
                .bundleId(po.getBundleId())
                .scope(AgreementScope.of(AgreementScene.parse(po.getScene()), po.getMarket(), po.getLocale(),
                        po.getBrandCode(), po.getCarLineCode()))
                .publishedAt(po.getPublishedAt())
                .privacyPolicy(AgreementRevisionConverter.privacyOf(po))
                .userAgreement(AgreementRevisionConverter.userAgreementOf(po))
                .build();
    }
}
