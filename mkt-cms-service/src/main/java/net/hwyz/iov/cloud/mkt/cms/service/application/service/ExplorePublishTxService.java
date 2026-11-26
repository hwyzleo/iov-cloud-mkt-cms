package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExplorePagePublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageCurrentRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 探索页发布事务服务。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.2：同一数据库事务内完成“锁频道指针 → 重新检查幂等 →
 * 比较 expectedReleaseId → 校验待生效冲突 → 物化发布版本 → 切换指针 → 写审计”，
 * 任一写入失败整体回滚；不引入 Kafka、Outbox 或分布式事务。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExplorePublishTxService {

    private final ExplorePageRepository pageRepository;
    private final ExplorePageCurrentRepository currentRepository;
    private final ExplorePageContentService contentService;
    private final ExploreOperationRepository operationRepository;
    private final Clock clock;

    /**
     * 发布事务
     *
     * @param cmd                      发布命令
     * @param requestHash             规范化命令哈希
     * @param expectedDraftLockVersion 哈希时的草稿乐观锁版本
     * @return 发布版本
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public ExplorePage publish(ExplorePagePublishCmd cmd, String requestHash,
                               int expectedDraftLockVersion) {
        String channelCode = cmd.getChannelCode();
        ExplorePage existing = pageRepository.findByRequestId(cmd.getRequestId()).orElse(null);
        if (existing != null) {
            return requireSameCommand(existing, requestHash);
        }

        Instant now = Instant.now(clock);
        ExplorePageCurrent current = lockOrCreateCurrent(channelCode, now);

        // 锁等待后必须看到最新已提交数据，不能用旧快照判断幂等与当前版本
        existing = pageRepository.findByRequestId(cmd.getRequestId()).orElse(null);
        if (existing != null) {
            return requireSameCommand(existing, requestHash);
        }
        if (!Objects.equals(cmd.getExpectedReleaseId(), current.getReleaseId())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                    "当前页面版本已变化，请重新读取后重试");
        }
        if (pageRepository.findScheduled(channelCode).isPresent()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_SCHEDULE_CONFLICT,
                    "频道已存在待生效版本，请先取消定时发布");
        }

        ExplorePage draft = pageRepository.findDraftForUpdate(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "频道尚无页面草稿，无法发布"));
        if (draft.getLockVersion() != expectedDraftLockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                    "页面草稿已变化，请重新读取后重试");
        }

        String releaseId = ExploreIdGenerator.releaseId();
        List<ExploreComponentData> content = contentService.materializePublish(draft, releaseId);
        int pageVersion = pageRepository.maxPageVersion(channelCode) + 1;
        ExplorePageStatus status = "SCHEDULED".equals(cmd.getPublishType())
                ? ExplorePageStatus.SCHEDULED : ExplorePageStatus.PUBLISHED;
        ExplorePage release = ExplorePage.releaseOf(draft, releaseId, pageVersion, status,
                cmd.getEffectiveAt(), cmd.getExpireAt(), cmd.getRequestId(), requestHash,
                cmd.getExpectedReleaseId(), cmd.getOperator(), now);
        pageRepository.insert(release);
        for (ExploreComponentData data : content) {
            pageRepository.insertComponent(data.getComponent());
            for (ExplorePageItem item : data.getItems()) {
                pageRepository.insertItem(item);
            }
        }
        if (status == ExplorePageStatus.PUBLISHED) {
            int switched = currentRepository.switchPointer(channelCode, current.getLockVersion(),
                    releaseId, now);
            if (switched == 0) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                        "当前页面版本已被并发切换，发布整体回滚");
            }
        }
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                ExploreAction.PUBLISH_PAGE, "EXPLORE_PAGE", releaseId, cmd.getOperator(), now,
                cmd.getRequestId(), TraceContext.currentTraceId(),
                "channelCode=" + channelCode + ", pageVersion=" + pageVersion + ", status=" + status));
        log.info("探索页发布成功: channelCode={}, releaseId={}, pageVersion={}, status={}",
                channelCode, releaseId, pageVersion, status);
        return release;
    }

    /**
     * 获取或初始化频道当前指针并加锁
     *
     * @param channelCode 频道编码
     * @param now         服务端时间
     * @return 已锁定的当前指针
     */
    public ExplorePageCurrent lockOrCreateCurrent(String channelCode, Instant now) {
        ExplorePageCurrent current = currentRepository.lock(channelCode).orElse(null);
        if (current != null) {
            return current;
        }
        currentRepository.insertEmpty(channelCode, now);
        return currentRepository.lock(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_TEMPORARILY_UNAVAILABLE,
                        "频道当前指针初始化失败"));
    }

    private ExplorePage requireSameCommand(ExplorePage existing, String requestHash) {
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_IDEMPOTENCY_CONFLICT,
                    "同一 requestId 已用于不同发布命令");
        }
        return existing;
    }
}
