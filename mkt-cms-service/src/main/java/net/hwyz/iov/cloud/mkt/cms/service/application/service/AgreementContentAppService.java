package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.application.assembler.AgreementAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementScopeQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleContentResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.CurrentBundleView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementBundleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementRevisionRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ScopeResolver;
import org.springframework.stereotype.Service;

/**
 * 协议内容读取应用服务（内部业务读取 + 管理侧修订查看）。
 * <p>
 * MKT-CMS-DSN-CR-001 §6：当前包查询必须匹配完整五维 Scope，缺省补齐不得实现成“查不到就回退”；
 * 历史修订只有被至少一个已提交发布包引用时业务侧才可读；读取不产生任何同意、绑定或激活副作用。
 *
 * @author hwyz_leo
 */
@Service
@RequiredArgsConstructor
public class AgreementContentAppService {

    private final ScopeResolver scopeResolver;
    private final AgreementBundleRepository agreementBundleRepository;
    private final AgreementRevisionRepository agreementRevisionRepository;

    /**
     * 按范围读取当前完整发布包（一次一致性读取）
     *
     * @param query 范围查询
     * @return 当前发布包与两份正文
     */
    public AgreementBundleContentResult getCurrentBundle(AgreementScopeQuery query) {
        AgreementScope scope = scopeResolver.resolve(query.getScene(), query.getMarket(), query.getLocale(),
                query.getBrandCode(), query.getCarLineCode());
        CurrentBundleView view = agreementBundleRepository.findCurrentView(scope)
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_CURRENT_NOT_FOUND,
                        "范围[" + scope.identityKey() + "]没有当前协议发布包"));
        return AgreementAssembler.toBundleContentResult(view);
    }

    /**
     * 按修订标识读取已发布历史正文
     *
     * @param revisionId 修订标识
     * @return 单份协议内容
     */
    public AgreementBundleContentResult.AgreementContentResult getPublishedRevision(String revisionId) {
        requireRevisionId(revisionId);
        AgreementRevision revision = agreementRevisionRepository.findPublishedByRevisionId(revisionId)
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND,
                        "修订不存在或尚未通过发布包发布: " + revisionId));
        return AgreementAssembler.toContentResult(revision);
    }

    /**
     * 管理侧查看修订（可查看未发布修订，不暴露给普通业务读取方）
     *
     * @param revisionId 修订标识
     * @return 修订出参
     */
    public AgreementRevisionResult getRevisionForManagement(String revisionId) {
        requireRevisionId(revisionId);
        AgreementRevision revision = agreementRevisionRepository.findByRevisionId(revisionId)
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND,
                        "修订不存在: " + revisionId));
        return AgreementAssembler.toRevisionResult(revision);
    }

    private void requireRevisionId(String revisionId) {
        if (revisionId == null || revisionId.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "修订标识不能为空");
        }
    }
}
