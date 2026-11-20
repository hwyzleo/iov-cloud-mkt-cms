package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.assembler.AgreementAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.PublishAgreementBundleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementBundleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.PublishRequestHash;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ScopeResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * 协议发布应用服务（发布编排：参数规范化、幂等哈希、并发竞争重试）。
 * <p>
 * MKT-CMS-DSN-CR-001 §5：相同 requestId 的请求必须复用原命令；
 * requestId 唯一键竞争时事务回滚后读取胜出记录并比较哈希，不在已失败事务内继续写入；
 * 锁竞争（唯一键建立指针行、死锁检测）由外层有限重试兜底，不静默覆盖。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementPublishAppService {

    private static final int REQUEST_ID_MAX_LENGTH = 64;

    private static final int MAX_ATTEMPTS = 3;

    private final ScopeResolver scopeResolver;
    private final AgreementBundleRepository agreementBundleRepository;
    private final AgreementTxService agreementTxService;

    /**
     * 成套发布协议包
     *
     * @param cmd 发布命令
     * @return 发布包出参（重复请求返回既有发布包）
     */
    public AgreementBundleResult publish(PublishAgreementBundleCmd cmd) {
        String operator = requireOperator(cmd.getOperator());
        validateRequestId(cmd.getRequestId());
        if (!cmd.isExpectedBundleIdPresent()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "expectedBundleId 字段必须出现（首次发布请显式传 null）");
        }
        validateExpectedBundleId(cmd.getExpectedBundleId());
        validateRevisionIds(cmd);

        AgreementScope scope = scopeResolver.resolve(cmd.getScene(), cmd.getMarket(), cmd.getLocale(),
                cmd.getBrandCode(), cmd.getCarLineCode());
        String requestHash = PublishRequestHash.of(scope, cmd.getPrivacyRevisionId(),
                cmd.getUserAgreementRevisionId(), cmd.getExpectedBundleId());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                AgreementBundle bundle = agreementTxService.publish(scope, requestHash, cmd);
                return AgreementAssembler.toBundleResult(bundle);
            } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
                AgreementBundleResult resolved = resolveAfterConflict(cmd.getRequestId(), requestHash);
                if (resolved != null) {
                    return resolved;
                }
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("协议发布并发竞争重试耗尽: requestId={}, attempts={}", cmd.getRequestId(), attempt);
                    throw new AgreementException(AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE,
                            "发布并发竞争冲突，请重试", e);
                }
                log.info("协议发布并发竞争，重试: requestId={}, attempt={}", cmd.getRequestId(), attempt);
            }
        }
        throw new AgreementException(AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE, "发布失败，请重试");
    }

    private AgreementBundleResult resolveAfterConflict(String requestId, String requestHash) {
        AgreementBundle winner = agreementBundleRepository.findByRequestId(requestId).orElse(null);
        if (winner == null) {
            return null;
        }
        if (!winner.getRequestHash().equals(requestHash)) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_IDEMPOTENCY_CONFLICT,
                    "同一 requestId 已用于不同发布命令");
        }
        return AgreementAssembler.toBundleResult(winner);
    }

    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "requestId 不能为空");
        }
        if (requestId.trim().length() > REQUEST_ID_MAX_LENGTH) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "requestId 长度不得超过 " + REQUEST_ID_MAX_LENGTH);
        }
    }

    private void validateExpectedBundleId(String expectedBundleId) {
        if (expectedBundleId != null && expectedBundleId.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "expectedBundleId 不接受空串，首次发布请显式传 null");
        }
    }

    private void validateRevisionIds(PublishAgreementBundleCmd cmd) {
        String privacy = cmd.getPrivacyRevisionId();
        String user = cmd.getUserAgreementRevisionId();
        if (privacy == null || privacy.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "隐私政策修订标识不能为空");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "用户协议修订标识不能为空");
        }
        if (privacy.trim().equals(user.trim())) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "隐私政策与用户协议必须引用不同修订");
        }
    }

    private String requireOperator(String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "无法从安全上下文识别操作人");
        }
        return operator.trim();
    }
}
