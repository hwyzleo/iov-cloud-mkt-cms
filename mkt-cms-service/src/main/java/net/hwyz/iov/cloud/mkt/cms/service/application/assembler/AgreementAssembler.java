package net.hwyz.iov.cloud.mkt.cms.service.application.assembler;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleContentResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementDraftResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionResult;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.CurrentBundleView;

import java.util.Arrays;

/**
 * 协议子域装配器（Domain → DTO）。
 *
 * @author hwyz_leo
 */
public final class AgreementAssembler {

    private AgreementAssembler() {
    }

    /**
     * 草稿领域对象转出参
     *
     * @param draft    草稿
     * @param adjusted 本次保存是否发生清洗调整
     * @return 草稿出参
     */
    public static AgreementDraftResult toDraftResult(AgreementDraft draft, boolean adjusted) {
        AgreementScope scope = draft.getScope();
        return AgreementDraftResult.builder()
                .draftId(draft.getDraftId())
                .agreementType(draft.getType().name())
                .scene(scope.getScene().name())
                .market(scope.getMarket())
                .locale(scope.getLocale())
                .brandCode(scope.getBrandCode())
                .carLineCode(scope.getCarLineCode())
                .title(draft.getTitle())
                .version(draft.getVersion())
                .agreementDate(draft.getAgreementDate())
                .contentType(draft.getContentType())
                .body(draft.getBody())
                .sanitizerVersion(draft.getSanitizerVersion())
                .adjusted(adjusted)
                .lockVersion(draft.getLockVersion())
                .updatedBy(draft.getUpdatedBy())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }

    /**
     * 修订领域对象转出参
     *
     * @param revision 修订
     * @return 修订出参
     */
    public static AgreementRevisionResult toRevisionResult(AgreementRevision revision) {
        AgreementScope scope = revision.getScope();
        return AgreementRevisionResult.builder()
                .revisionId(revision.getRevisionId())
                .agreementCode(revision.getAgreementCode())
                .agreementType(revision.getType().name())
                .scene(scope.getScene().name())
                .market(scope.getMarket())
                .locale(scope.getLocale())
                .brandCode(scope.getBrandCode())
                .carLineCode(scope.getCarLineCode())
                .title(revision.getTitle())
                .version(revision.getVersion())
                .agreementDate(revision.getAgreementDate())
                .contentType(revision.getContentType())
                .body(revision.getBody())
                .contentHash(revision.prefixedContentHash())
                .sanitizerVersion(revision.getSanitizerVersion())
                .sourceDraftId(revision.getSourceDraftId())
                .sourceDraftVersion(revision.getSourceDraftVersion())
                .frozenBy(revision.getFrozenBy())
                .frozenAt(revision.getFrozenAt())
                .build();
    }

    /**
     * 发布包领域对象转出参
     *
     * @param bundle 发布包
     * @return 发布包出参
     */
    public static AgreementBundleResult toBundleResult(AgreementBundle bundle) {
        AgreementScope scope = bundle.getScope();
        return AgreementBundleResult.builder()
                .bundleId(bundle.getBundleId())
                .scene(scope.getScene().name())
                .market(scope.getMarket())
                .locale(scope.getLocale())
                .brandCode(scope.getBrandCode())
                .carLineCode(scope.getCarLineCode())
                .privacyRevisionId(bundle.getPrivacyRevisionId())
                .userRevisionId(bundle.getUserRevisionId())
                .publishedBy(bundle.getPublishedBy())
                .publishedAt(bundle.getPublishedAt())
                .requestId(bundle.getRequestId())
                .requestHash(bundle.getRequestHash())
                .expectedBundleId(bundle.getExpectedBundleId())
                .build();
    }

    /**
     * 当前发布包视图转读取结果（固定两项协议）
     *
     * @param view 当前发布包视图
     * @return 含两份正文的读取结果
     */
    public static AgreementBundleContentResult toBundleContentResult(CurrentBundleView view) {
        AgreementScope scope = view.getScope();
        return AgreementBundleContentResult.builder()
                .bundleId(view.getBundleId())
                .scene(scope.getScene().name())
                .market(scope.getMarket())
                .locale(scope.getLocale())
                .brandCode(scope.getBrandCode())
                .carLineCode(scope.getCarLineCode())
                .publishedAt(view.getPublishedAt())
                .agreements(Arrays.asList(
                        toContentResult(view.getPrivacyPolicy()),
                        toContentResult(view.getUserAgreement())))
                .build();
    }

    /**
     * 单份修订转内容读取结果
     *
     * @param revision 修订
     * @return 单份协议内容
     */
    public static AgreementBundleContentResult.AgreementContentResult toContentResult(AgreementRevision revision) {
        AgreementScope scope = revision.getScope();
        return AgreementBundleContentResult.AgreementContentResult.builder()
                .agreementCode(revision.getAgreementCode())
                .agreementType(revision.getType().name())
                .revisionId(revision.getRevisionId())
                .version(revision.getVersion())
                .agreementDate(revision.getAgreementDate())
                .title(revision.getTitle())
                .scene(scope.getScene().name())
                .market(scope.getMarket())
                .locale(scope.getLocale())
                .brandCode(scope.getBrandCode())
                .carLineCode(scope.getCarLineCode())
                .contentType(revision.getContentType())
                .body(revision.getBody())
                .contentHash(revision.prefixedContentHash())
                .sanitizerVersion(revision.getSanitizerVersion())
                .build();
    }
}
