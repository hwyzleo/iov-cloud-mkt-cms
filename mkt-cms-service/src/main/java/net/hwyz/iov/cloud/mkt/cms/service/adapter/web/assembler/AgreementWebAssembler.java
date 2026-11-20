package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler;

import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementBundleResponse;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementContentResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.CreateAgreementDraftRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.PublishAgreementBundleRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.UpdateAgreementDraftRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementDraftResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementPublishResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementRevisionResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.CreateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.PublishAgreementBundleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.UpdateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleContentResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementDraftResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 协议子域 Web 装配器（VO ⇄ DTO）。
 *
 * @author hwyz_leo
 */
public final class AgreementWebAssembler {

    private AgreementWebAssembler() {
    }

    /**
     * 新建草稿 VO 转命令
     *
     * @param request  请求
     * @param operator 安全上下文操作人
     * @return 命令
     */
    public static CreateAgreementDraftCmd toCmd(CreateAgreementDraftRequest request, String operator) {
        return CreateAgreementDraftCmd.builder()
                .agreementType(request.getAgreementType())
                .scene(request.getScene())
                .market(request.getMarket())
                .locale(request.getLocale())
                .brandCode(request.getBrandCode())
                .carLineCode(request.getCarLineCode())
                .title(request.getTitle())
                .version(request.getVersion())
                .agreementDate(request.getAgreementDate())
                .body(request.getBody())
                .operator(operator)
                .build();
    }

    /**
     * 修改草稿 VO 转命令
     *
     * @param draftId  草稿标识
     * @param request  请求
     * @param operator 安全上下文操作人
     * @return 命令
     */
    public static UpdateAgreementDraftCmd toCmd(String draftId, UpdateAgreementDraftRequest request, String operator) {
        return UpdateAgreementDraftCmd.builder()
                .draftId(draftId)
                .lockVersion(request.getLockVersion())
                .title(request.getTitle())
                .version(request.getVersion())
                .agreementDate(request.getAgreementDate())
                .body(request.getBody())
                .operator(operator)
                .build();
    }

    /**
     * 发布 VO 转命令
     *
     * @param request  请求
     * @param operator 安全上下文操作人
     * @return 命令
     */
    public static PublishAgreementBundleCmd toCmd(PublishAgreementBundleRequest request, String operator) {
        return PublishAgreementBundleCmd.builder()
                .scene(request.getScene())
                .market(request.getMarket())
                .locale(request.getLocale())
                .brandCode(request.getBrandCode())
                .carLineCode(request.getCarLineCode())
                .privacyRevisionId(request.getPrivacyRevisionId())
                .userAgreementRevisionId(request.getUserAgreementRevisionId())
                .requestId(request.getRequestId())
                .expectedBundleId(request.getExpectedBundleId())
                .expectedBundleIdPresent(request.isExpectedBundleIdPresent())
                .operator(operator)
                .build();
    }

    /**
     * 草稿出参转 VO
     *
     * @param result 出参
     * @return VO
     */
    public static AgreementDraftResponse toVo(AgreementDraftResult result) {
        return AgreementDraftResponse.builder()
                .draftId(result.getDraftId())
                .agreementType(result.getAgreementType())
                .scene(result.getScene())
                .market(result.getMarket())
                .locale(result.getLocale())
                .brandCode(result.getBrandCode())
                .carLineCode(result.getCarLineCode())
                .title(result.getTitle())
                .version(result.getVersion())
                .agreementDate(result.getAgreementDate())
                .contentType(result.getContentType())
                .body(result.getBody())
                .sanitizerVersion(result.getSanitizerVersion())
                .adjusted(result.isAdjusted())
                .lockVersion(result.getLockVersion())
                .updatedBy(result.getUpdatedBy())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    /**
     * 修订出参转 VO
     *
     * @param result 出参
     * @return VO
     */
    public static AgreementRevisionResponse toVo(AgreementRevisionResult result) {
        return AgreementRevisionResponse.builder()
                .revisionId(result.getRevisionId())
                .agreementCode(result.getAgreementCode())
                .agreementType(result.getAgreementType())
                .scene(result.getScene())
                .market(result.getMarket())
                .locale(result.getLocale())
                .brandCode(result.getBrandCode())
                .carLineCode(result.getCarLineCode())
                .title(result.getTitle())
                .version(result.getVersion())
                .agreementDate(result.getAgreementDate())
                .contentType(result.getContentType())
                .body(result.getBody())
                .contentHash(result.getContentHash())
                .sanitizerVersion(result.getSanitizerVersion())
                .sourceDraftId(result.getSourceDraftId())
                .sourceDraftVersion(result.getSourceDraftVersion())
                .frozenBy(result.getFrozenBy())
                .frozenAt(result.getFrozenAt())
                .build();
    }

    /**
     * 发布出参转 VO
     *
     * @param result 出参
     * @return VO
     */
    public static AgreementPublishResponse toVo(AgreementBundleResult result) {
        return AgreementPublishResponse.builder()
                .bundleId(result.getBundleId())
                .scene(result.getScene())
                .market(result.getMarket())
                .locale(result.getLocale())
                .brandCode(result.getBrandCode())
                .carLineCode(result.getCarLineCode())
                .privacyRevisionId(result.getPrivacyRevisionId())
                .userRevisionId(result.getUserRevisionId())
                .publishedBy(result.getPublishedBy())
                .publishedAt(result.getPublishedAt())
                .requestId(result.getRequestId())
                .requestHash(result.getRequestHash())
                .expectedBundleId(result.getExpectedBundleId())
                .build();
    }

    /**
     * 当前发布包读取结果转内部服务出参（API 模块 VO）
     *
     * @param result 读取结果
     * @return 内部服务出参
     */
    public static AgreementBundleResponse toApiResponse(AgreementBundleContentResult result) {
        List<AgreementContentResponse> agreements = result.getAgreements().stream()
                .map(AgreementWebAssembler::toApiResponse)
                .collect(Collectors.toList());
        return AgreementBundleResponse.builder()
                .bundleId(result.getBundleId())
                .scene(result.getScene())
                .market(result.getMarket())
                .locale(result.getLocale())
                .brandCode(result.getBrandCode())
                .carLineCode(result.getCarLineCode())
                .publishedAt(result.getPublishedAt())
                .agreements(agreements)
                .build();
    }

    /**
     * 单份协议内容转内部服务出参（API 模块 VO）
     *
     * @param result 单份内容
     * @return 内部服务出参
     */
    public static AgreementContentResponse toApiResponse(AgreementBundleContentResult.AgreementContentResult result) {
        return AgreementContentResponse.builder()
                .agreementCode(result.getAgreementCode())
                .agreementType(result.getAgreementType())
                .revisionId(result.getRevisionId())
                .version(result.getVersion())
                .agreementDate(result.getAgreementDate())
                .title(result.getTitle())
                .scene(result.getScene())
                .market(result.getMarket())
                .locale(result.getLocale())
                .brandCode(result.getBrandCode())
                .carLineCode(result.getCarLineCode())
                .contentType(result.getContentType())
                .body(result.getBody())
                .contentHash(result.getContentHash())
                .sanitizerVersion(result.getSanitizerVersion())
                .build();
    }
}
