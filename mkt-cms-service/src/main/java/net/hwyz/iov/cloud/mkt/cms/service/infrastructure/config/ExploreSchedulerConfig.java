package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 探索页调度开关。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.2：首版使用 MySQL 状态机 + 行锁 + 批量扫描，每 30 秒执行；
 * 可通过 {@code cms.explore.scheduler-enabled=false} 在无需调度的环境关闭。
 *
 * @author hwyz_leo
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "cms.explore", name = "scheduler-enabled", havingValue = "true",
        matchIfMissing = true)
public class ExploreSchedulerConfig {
}
