package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.PublishAgreementBundleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.BizIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementDraft;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementBundleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementCurrentRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementDraftRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementRevisionRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.AgreementCodeRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * 协议子域事务服务（定稿与发布的事务边界）。
 * <p>
 * MKT-CMS-DSN-CR-001 §5.2：发布在<b>同一数据库事务</b>内完成“锁当前指针 → 重新检查幂等 →
 * 校验发布前提与两份修订 → 创建不可变发布包 → 切换指针 → 写成功操作记录”，任一写入失败整体回滚；
 * 事务方法不与 {@link DuplicateKeyException} 之类的失败处理混在一起，避免在已标记回滚的事务内继续写入。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementTxService {

    private final AgreementDraftRepository agreementDraftRepository;
    private final AgreementRevisionRepository agreementRevisionRepository;
    private final AgreementBundleRepository agreementBundleRepository;
    private final AgreementCurrentRepository agreementCurrentRepository;
    private final AgreementOperationRepository agreementOperationRepository;

    /**
     * 定稿事务：以“草稿 + 草稿乐观锁版本”为幂等依据生成不可变修订
     *
     * @param draftId     草稿标识
     * @param lockVersion 期望的草稿乐观锁版本
     * @param operator    操作人
     * @return 修订（重复定稿返回同一修订）
     */
    @Transactional(rollbackFor = Exception.class)
    public AgreementRevision freeze(String draftId, int lockVersion, String operator) {
        AgreementDraft draft = agreementDraftRepository.findByDraftId(draftId)
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND));

        AgreementRevision existing = agreementRevisionRepository.findBySourceDraft(draftId, lockVersion).orElse(null);
        if (existing != null) {
            return existing;
        }
        if (draft.getLockVersion() != lockVersion) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT,
                    "草稿已被修改，请基于最新乐观锁版本重新定稿");
        }

        Instant now = Instant.now();
        AgreementRevision revision = AgreementRevision.freezeOf(draft, BizIdGenerator.revisionId(),
                AgreementCodeRule.of(draft.getType(), draft.getScope()), operator, now);
        agreementRevisionRepository.insert(revision);
        agreementOperationRepository.insert(AgreementOperation.success(BizIdGenerator.operationId(),
                AgreementAction.FREEZE, "AGREEMENT_REVISION", revision.getRevisionId(), operator, now,
                TraceContext.currentTraceId()));
        log.info("协议定稿成功: draftId={}, lockVersion={}, revisionId={}, agreementCode={}",
                draftId, lockVersion, revision.getRevisionId(), revision.getAgreementCode());
        return revision;
    }

    /**
     * 发布事务：成套发布两份修订并原子切换当前指针
     *
     * @param scope       规范化适用范围
     * @param requestHash 规范化业务命令哈希
     * @param cmd         发布命令
     * @return 发布包（重复请求返回既有发布包）
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public AgreementBundle publish(AgreementScope scope, String requestHash, PublishAgreementBundleCmd cmd) {
        AgreementBundle existing = agreementBundleRepository.findByRequestId(cmd.getRequestId()).orElse(null);
        if (existing != null) {
            return requireSameCommand(existing, requestHash);
        }

        AgreementCurrent current = agreementCurrentRepository.lockByScope(scope).orElse(null);
        if (current == null) {
            Instant now = Instant.now();
            agreementCurrentRepository.insertEmpty(scope, now);
            current = agreementCurrentRepository.lockByScope(scope)
                    .orElseThrow(() -> new AgreementException(
                            AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE, "当前指针初始化失败"));
        }

        // 获得锁后重新检查幂等标识：必须使用锁读，锁等待期间可能有同 requestId 的请求已提交，
        // 不能继续使用旧事务快照（§5.2 第 4 步）
        existing = agreementBundleRepository.findByRequestIdForUpdate(cmd.getRequestId()).orElse(null);
        if (existing != null) {
            return requireSameCommand(existing, requestHash);
        }

        if (!Objects.equals(cmd.getExpectedBundleId(), current.getBundleId())) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT,
                    "当前发布包已变化，请重新读取后重试");
        }

        AgreementRevision privacy = agreementRevisionRepository
                .findByRevisionId(cmd.getPrivacyRevisionId())
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND,
                        "隐私政策修订不存在: " + cmd.getPrivacyRevisionId()));
        AgreementRevision userAgreement = agreementRevisionRepository
                .findByRevisionId(cmd.getUserAgreementRevisionId())
                .orElseThrow(() -> new AgreementException(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND,
                        "用户协议修订不存在: " + cmd.getUserAgreementRevisionId()));

        Instant now = Instant.now();
        String bundleId = BizIdGenerator.bundleId();
        AgreementBundle bundle = AgreementBundle.publishOf(bundleId, scope, privacy, userAgreement,
                cmd.getRequestId(), requestHash, cmd.getExpectedBundleId(), cmd.getOperator(), now);
        agreementBundleRepository.insert(bundle);

        int switched = agreementCurrentRepository.switchPointer(scope, current.getLockVersion(), bundleId, now);
        if (switched == 0) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT,
                    "当前指针已被并发切换，发布整体回滚");
        }

        agreementOperationRepository.insert(AgreementOperation.success(BizIdGenerator.operationId(),
                AgreementAction.PUBLISH, "AGREEMENT_BUNDLE", bundleId, cmd.getOperator(), now,
                TraceContext.currentTraceId()));
        log.info("协议发布成功: bundleId={}, scope={}, privacyRevision={}, userRevision={}",
                bundleId, scope.identityKey(), privacy.getRevisionId(), userAgreement.getRevisionId());
        return bundle;
    }

    private AgreementBundle requireSameCommand(AgreementBundle existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_IDEMPOTENCY_CONFLICT,
                    "同一 requestId 已用于不同发布命令");
        }
        return existing;
    }
}
