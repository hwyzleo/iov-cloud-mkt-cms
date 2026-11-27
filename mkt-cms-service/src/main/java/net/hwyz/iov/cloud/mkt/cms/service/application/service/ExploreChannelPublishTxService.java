package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 探索页频道发布事务服务（MKT-CMS-DSN-CR-005 §4.2 / D-019）。
 * <p>
 * 同一数据库事务内完成“检查幂等 → 锁定频道根 → 重查幂等 → 校验 lockVersion 与
 * expectedCurrentRevisionId → 草稿定稿 → 切换当前指针 → 写审计”，任一步失败整体回滚；
 * 不调用页面发布服务、不创建页面版本、不修改页面调度与当前页面指针。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreChannelPublishTxService {

    private final ExploreChannelRepository channelRepository;
    private final ExploreOperationRepository operationRepository;
    private final Clock clock;

    /**
     * 频道发布事务
     *
     * @param cmd         发布命令（已规范化）
     * @param requestHash 规范化命令哈希
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void publish(ExploreChannelPublishCmd cmd, String requestHash) {
        String channelCode = cmd.getChannelCode();
        ExploreChannelRevision existing = channelRepository.findByRequestId(cmd.getRequestId()).orElse(null);
        if (existing != null) {
            requireSameCommand(existing, requestHash);
            return;
        }

        ExploreChannel root = channelRepository.findByCodeForUpdate(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "频道不存在: " + channelCode));

        // 锁等待后必须看到最新已提交数据，不能用旧快照判断幂等与当前指针
        existing = channelRepository.findByRequestId(cmd.getRequestId()).orElse(null);
        if (existing != null) {
            requireSameCommand(existing, requestHash);
            return;
        }
        if (root.getLockVersion() != cmd.getLockVersion()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被修改，请基于最新乐观锁版本重试");
        }
        if (!Objects.equals(cmd.getExpectedCurrentRevisionId(), root.getCurrentRevisionId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                    "频道当前发布修订已变化，请重新读取后重试");
        }
        if (!StringUtils.hasText(root.getDraftRevisionId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                    "频道尚无草稿，无法发布");
        }
        ExploreChannelRevision draft = channelRepository.findRevision(root.getDraftRevisionId())
                .filter(revision -> revision.getRevisionStatus() == ExploreRevisionStatus.DRAFT)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                        "频道草稿状态已变化，无法发布"));
        if (!StringUtils.hasText(draft.getName()) || draft.getStatus() == null) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "频道草稿配置不完整，无法发布");
        }

        Instant now = Instant.now(clock);
        ExploreChannelRevision published = draft.published(cmd.getRequestId(), requestHash,
                cmd.getExpectedCurrentRevisionId(), cmd.getOperator(), now);
        if (channelRepository.publishRevision(published) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                    "频道草稿状态已变化，发布失败");
        }
        ExploreChannel updated = root.published(draft.getRevisionId(), draft.getName(), draft.getSortNo(),
                draft.getStatus(), cmd.getOperator(), now);
        if (channelRepository.update(updated, root.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被并发修改，发布整体回滚");
        }
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                ExploreAction.PUBLISH_CHANNEL, "EXPLORE_CHANNEL", channelCode, cmd.getOperator(), now,
                cmd.getRequestId(), TraceContext.currentTraceId(),
                "revisionId=" + draft.getRevisionId() + ", revisionNo=" + draft.getRevisionNo()));
        log.info("探索页频道发布成功: channelCode={}, revisionId={}, revisionNo={}",
                channelCode, draft.getRevisionId(), draft.getRevisionNo());
    }

    private void requireSameCommand(ExploreChannelRevision existing, String requestHash) {
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_IDEMPOTENCY_CONFLICT,
                    "同一 requestId 已用于不同发布命令");
        }
    }
}
