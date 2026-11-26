package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageCurrentRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * 探索页调度事务服务（定时生效/到期、取消定时、立即下线）。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.2：激活与到期都按 releaseId 比较后更新，重启或重复执行不重复切换；
 * 到期后无当前版本，不回退旧版本。锁顺序统一为“频道指针 → 页面版本”，避免死锁。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreScheduleTxService {

    private static final String SYSTEM_OPERATOR = "system-scheduler";

    private final ExplorePageRepository pageRepository;
    private final ExplorePageCurrentRepository currentRepository;
    private final ExploreOperationRepository operationRepository;
    private final ExplorePublishTxService publishTxService;
    private final Clock clock;

    /**
     * 取消待生效版本
     *
     * @param channelCode 频道编码
     * @param operator    操作人
     * @return 已取消的版本
     */
    @Transactional(rollbackFor = Exception.class)
    public ExplorePage cancelSchedule(String channelCode, String operator) {
        ExplorePage scheduled = pageRepository.findScheduled(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_SCHEDULE_CONFLICT,
                        "频道没有待生效版本"));
        ExplorePage locked = pageRepository.findByIdForUpdate(scheduled.getPageId())
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "待生效版本不存在"));
        if (locked.getStatus() != ExplorePageStatus.SCHEDULED) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_SCHEDULE_CONFLICT, "待生效版本状态已变化");
        }
        Instant now = Instant.now(clock);
        ExplorePage canceled = locked.withStatus(ExplorePageStatus.CANCELED, operator, now);
        pageRepository.updateStatus(canceled);
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                ExploreAction.CANCEL_SCHEDULE, "EXPLORE_PAGE", canceled.getPageId(), operator, now,
                null, TraceContext.currentTraceId(), "channelCode=" + channelCode));
        log.info("探索页定时发布已取消: channelCode={}, releaseId={}", channelCode, canceled.getPageId());
        return canceled;
    }

    /**
     * 立即下线频道页面并取消待生效版本
     *
     * @param channelCode 频道编码
     * @param operator    操作人
     * @return 被下线的当前版本（无当前版本时返回 null）
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public ExplorePage offline(String channelCode, String operator) {
        Instant now = Instant.now(clock);
        ExplorePageCurrent current = publishTxService.lockOrCreateCurrent(channelCode, now);
        ExplorePage scheduled = pageRepository.findScheduled(channelCode).orElse(null);
        if (scheduled != null) {
            ExplorePage lockedScheduled = pageRepository.findByIdForUpdate(scheduled.getPageId())
                    .orElse(scheduled);
            if (lockedScheduled.getStatus() == ExplorePageStatus.SCHEDULED) {
                pageRepository.updateStatus(lockedScheduled.withStatus(
                        ExplorePageStatus.CANCELED, operator, now));
            }
        }
        if (current.getReleaseId() == null) {
            if (scheduled == null) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "频道当前没有可下线的页面");
            }
            return null;
        }
        ExplorePage release = pageRepository.findByIdForUpdate(current.getReleaseId())
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "当前页面版本不存在"));
        ExplorePage offline = release.offline(operator, now);
        pageRepository.updateStatus(offline);
        if (currentRepository.clearPointer(channelCode, current.getLockVersion(), now) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                    "当前页面版本已被并发切换，下线整体回滚");
        }
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                ExploreAction.OFFLINE_PAGE, "EXPLORE_PAGE", release.getPageId(), operator, now,
                null, TraceContext.currentTraceId(), "channelCode=" + channelCode));
        log.info("探索页已立即下线: channelCode={}, releaseId={}", channelCode, release.getPageId());
        return offline;
    }

    /**
     * 激活到期的定时版本（调度器）
     *
     * @param releaseId 定时版本标识
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void activate(String releaseId) {
        ExplorePage scheduled = pageRepository.findById(releaseId).orElse(null);
        if (scheduled == null || scheduled.getStatus() != ExplorePageStatus.SCHEDULED) {
            return;
        }
        Instant now = Instant.now(clock);
        ExplorePageCurrent current = publishTxService.lockOrCreateCurrent(scheduled.getChannelCode(), now);
        ExplorePage locked = pageRepository.findByIdForUpdate(releaseId).orElse(null);
        if (locked == null || locked.getStatus() != ExplorePageStatus.SCHEDULED
                || locked.getEffectiveAt() == null || locked.getEffectiveAt().isAfter(now)) {
            return;
        }
        if (locked.getExpireAt() != null && !locked.getExpireAt().isAfter(now)) {
            pageRepository.updateStatus(locked.withStatus(ExplorePageStatus.EXPIRED, SYSTEM_OPERATOR, now));
            return;
        }
        if (currentRepository.activate(scheduled.getChannelCode(), current.getReleaseId(), releaseId, now) == 0) {
            log.info("探索页定时激活跳过（指针已变化）: releaseId={}", releaseId);
            return;
        }
        ExplorePage activated = locked.toBuilder()
                .status(ExplorePageStatus.PUBLISHED)
                .publishedAt(now)
                .modifyBy(SYSTEM_OPERATOR)
                .modifyTime(now)
                .build();
        pageRepository.updateStatus(activated);
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                ExploreAction.SCHEDULE_ACTIVATE, "EXPLORE_PAGE", releaseId, SYSTEM_OPERATOR, now,
                null, TraceContext.currentTraceId(), "channelCode=" + scheduled.getChannelCode()));
        log.info("探索页定时版本已生效: channelCode={}, releaseId={}",
                scheduled.getChannelCode(), releaseId);
    }

    /**
     * 清除已到期当前版本（调度器）
     *
     * @param releaseId 当前版本标识
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void expire(String releaseId) {
        ExplorePage page = pageRepository.findById(releaseId).orElse(null);
        if (page == null || page.getStatus() != ExplorePageStatus.PUBLISHED) {
            return;
        }
        Instant now = Instant.now(clock);
        ExplorePageCurrent current = publishTxService.lockOrCreateCurrent(page.getChannelCode(), now);
        if (!releaseId.equals(current.getReleaseId())) {
            return;
        }
        ExplorePage locked = pageRepository.findByIdForUpdate(releaseId).orElse(null);
        if (locked == null || locked.getExpireAt() == null || locked.getExpireAt().isAfter(now)) {
            return;
        }
        if (currentRepository.expire(page.getChannelCode(), releaseId, now) == 0) {
            return;
        }
        pageRepository.updateStatus(locked.withStatus(ExplorePageStatus.EXPIRED, SYSTEM_OPERATOR, now));
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                ExploreAction.SCHEDULE_EXPIRE, "EXPLORE_PAGE", releaseId, SYSTEM_OPERATOR, now,
                null, TraceContext.currentTraceId(), "channelCode=" + page.getChannelCode()));
        log.info("探索页当前版本已到期: channelCode={}, releaseId={}",
                page.getChannelCode(), releaseId);
    }
}
