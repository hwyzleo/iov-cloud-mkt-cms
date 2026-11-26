package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreScheduleTxService;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config.ExploreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 探索页定时生效/到期调度器。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.2 / D-013：每 30 秒按索引批量扫描到期记录，
 * 在同一事务中激活待生效版本或清除已到期当前指针；重复执行或重启不重复切换。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cms.explore", name = "scheduler-enabled", havingValue = "true",
        matchIfMissing = true)
public class ExplorePageScheduler {

    private final ExplorePageRepository pageRepository;
    private final ExploreScheduleTxService scheduleTxService;
    private final ExploreProperties properties;
    private final Clock clock;

    /**
     * 定时扫描（默认 30 秒）
     */
    @Scheduled(fixedDelayString = "${cms.explore.scheduler-interval-ms:30000}")
    public void scan() {
        Instant now = Instant.now(clock);
        int batchSize = Math.max(1, properties.getSchedulerBatchSize());
        try {
            List<ExplorePage> due = pageRepository.findDueSchedules(now, batchSize);
            for (ExplorePage page : due) {
                try {
                    scheduleTxService.activate(page.getPageId());
                } catch (RuntimeException e) {
                    log.warn("探索页定时激活失败: releaseId={}, message={}", page.getPageId(), e.getMessage());
                }
            }
            List<ExplorePage> expired = pageRepository.findExpiredCurrent(now, batchSize);
            for (ExplorePage page : expired) {
                try {
                    scheduleTxService.expire(page.getPageId());
                } catch (RuntimeException e) {
                    log.warn("探索页到期清除失败: releaseId={}, message={}", page.getPageId(), e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            log.warn("探索页调度扫描失败: {}", e.getMessage());
        }
    }
}
