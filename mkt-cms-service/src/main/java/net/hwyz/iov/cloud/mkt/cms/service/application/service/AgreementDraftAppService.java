package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.assembler.AgreementAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.CreateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.FreezeAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.UpdateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementDraftResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.BizIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.HtmlSanitizer;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementDraftRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementRevisionRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ScopeResolver;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 协议草稿应用服务（创建、修改、查询与定稿编排）。
 * <p>
 * MKT-CMS-DSN-CR-001 §4：草稿可多次编辑，定稿产生独立不可变快照；
 * 同一草稿版本重复定稿返回同一修订；正文修改后必须提交未被占用的新业务版本。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementDraftAppService {

    private final ScopeResolver scopeResolver;
    private final HtmlSanitizer htmlSanitizer;
    private final AgreementDraftRepository agreementDraftRepository;
    private final AgreementRevisionRepository agreementRevisionRepository;
    private final AgreementOperationRepository agreementOperationRepository;
    private final AgreementTxService agreementTxService;

    /**
     * 新建草稿
     *
     * @param cmd 新建命令
     * @return 草稿出参（含清洗后的实际正文）
     */
    @Transactional(rollbackFor = Exception.class)
    public AgreementDraftResult create(CreateAgreementDraftCmd cmd) {
        String operator = requireOperator(cmd.getOperator());
        AgreementType type = AgreementType.parse(cmd.getAgreementType());
        AgreementScope scope = scopeResolver.resolve(cmd.getScene(), cmd.getMarket(), cmd.getLocale(),
                cmd.getBrandCode(), cmd.getCarLineCode());
        SanitizedHtml sanitized = htmlSanitizer.sanitize(cmd.getBody());
        Instant now = Instant.now();

        AgreementDraft draft = AgreementDraft.create(BizIdGenerator.draftId(), type, scope,
                cmd.getTitle(), cmd.getVersion(), cmd.getAgreementDate(), sanitized, operator, now);
        agreementDraftRepository.insert(draft);
        agreementOperationRepository.insert(AgreementOperation.success(BizIdGenerator.operationId(),
                AgreementAction.CREATE_DRAFT, "AGREEMENT_DRAFT", draft.getDraftId(), operator, now,
                TraceContext.currentTraceId()));
        log.info("协议草稿创建成功: draftId={}, type={}, scope={}, version={}",
                draft.getDraftId(), type, scope.identityKey(), draft.getVersion());
        return AgreementAssembler.toDraftResult(draft, sanitized.isAdjusted());
    }

    /**
     * 修改草稿（范围与类型不可变，必须携带乐观锁版本）
     *
     * @param cmd 修改命令
     * @return 草稿出参（含清洗后的实际正文）
     */
    @Transactional(rollbackFor = Exception.class)
    public AgreementDraftResult update(UpdateAgreementDraftCmd cmd) {
        String operator = requireOperator(cmd.getOperator());
        AgreementDraft draft = agreementDraftRepository.findByDraftId(cmd.getDraftId())
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND));
        if (cmd.getLockVersion() == null || cmd.getLockVersion() != draft.getLockVersion()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT,
                    "草稿已被修改，请基于最新乐观锁版本重试");
        }
        SanitizedHtml sanitized = cmd.getBody() == null ? null : htmlSanitizer.sanitize(cmd.getBody());
        Instant now = Instant.now();
        AgreementDraft edited = draft.edited(cmd.getTitle(), cmd.getVersion(), cmd.getAgreementDate(),
                sanitized, operator, now);
        int updated = agreementDraftRepository.updateContent(edited, cmd.getLockVersion());
        if (updated == 0) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT,
                    "草稿已被并发修改，请基于最新乐观锁版本重试");
        }
        agreementOperationRepository.insert(AgreementOperation.success(BizIdGenerator.operationId(),
                AgreementAction.UPDATE_DRAFT, "AGREEMENT_DRAFT", edited.getDraftId(), operator, now,
                TraceContext.currentTraceId()));
        log.info("协议草稿修改成功: draftId={}, lockVersion={}", edited.getDraftId(), edited.getLockVersion());
        return AgreementAssembler.toDraftResult(edited, sanitized != null && sanitized.isAdjusted());
    }

    /**
     * 查询草稿
     *
     * @param draftId 草稿标识
     * @return 草稿出参
     */
    public AgreementDraftResult get(String draftId) {
        AgreementDraft draft = agreementDraftRepository.findByDraftId(draftId)
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND));
        return AgreementAssembler.toDraftResult(draft, false);
    }

    /**
     * 定稿（重复定稿幂等；并发唯一键竞争回滚后读取胜出修订）
     *
     * @param cmd 定稿命令
     * @return 修订出参
     */
    public AgreementRevisionResult freeze(FreezeAgreementDraftCmd cmd) {
        String operator = requireOperator(cmd.getOperator());
        if (cmd.getLockVersion() == null) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "定稿必须携带 lockVersion");
        }
        AgreementRevision existing = agreementRevisionRepository
                .findBySourceDraft(cmd.getDraftId(), cmd.getLockVersion()).orElse(null);
        if (existing != null) {
            return AgreementAssembler.toRevisionResult(existing);
        }
        try {
            return AgreementAssembler.toRevisionResult(
                    agreementTxService.freeze(cmd.getDraftId(), cmd.getLockVersion(), operator));
        } catch (DuplicateKeyException e) {
            // 并发定稿竞争：事务已回滚，重新读取胜出修订后比较业务语义
            AgreementRevision winner = agreementRevisionRepository
                    .findBySourceDraft(cmd.getDraftId(), cmd.getLockVersion()).orElse(null);
            if (winner != null) {
                return AgreementAssembler.toRevisionResult(winner);
            }
            throw new AgreementException(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT,
                    "该协议身份下的业务版本已被其它正文占用");
        }
    }

    private String requireOperator(String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "无法从安全上下文识别操作人");
        }
        return operator.trim();
    }
}
