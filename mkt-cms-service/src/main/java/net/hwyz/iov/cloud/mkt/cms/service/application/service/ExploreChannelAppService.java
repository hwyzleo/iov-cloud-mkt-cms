package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelCmd;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreGuard;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 探索页频道应用服务。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.1 / AC-029：频道编码稳定不可改，名称、排序与启停可维护；
 * 手机端只返回已启用且存在当前有效页面的频道。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreChannelAppService {

    private final ExploreChannelRepository channelRepository;
    private final ExploreOperationRepository operationRepository;
    private final Clock clock;

    /**
     * 全部频道（管理端）
     *
     * @return 频道列表
     */
    public List<ExploreChannel> list() {
        return channelRepository.findAll();
    }

    /**
     * 维护频道名称、排序与启停状态（编码不可改）
     *
     * @param cmd 维护命令
     * @return 更新后的频道
     */
    @Transactional(rollbackFor = Exception.class)
    public ExploreChannel update(ExploreChannelCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String channelCode = ExploreGuard.requireText(cmd.getChannelCode(), "频道编码", 32);
        ExploreChannel channel = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                        "频道不存在: " + channelCode));
        int expectedLockVersion = ExploreGuard.requireLockVersion(cmd.getLockVersion());
        if (channel.getLockVersion() != expectedLockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被修改，请基于最新乐观锁版本重试");
        }
        String name = ExploreGuard.requireText(cmd.getName(), "频道名称", 64);
        if (cmd.getSortNo() == null || cmd.getSortNo() < 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "频道排序不能为空且不得为负");
        }
        ExploreChannelStatus status = ExploreChannelStatus.parse(cmd.getStatus());
        Instant now = Instant.now(clock);
        ExploreChannel updated = channel.updated(name, cmd.getSortNo(), status, operator, now);
        if (channelRepository.update(updated, expectedLockVersion) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "频道已被并发修改，请基于最新乐观锁版本重试");
        }
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                ExploreAction.UPDATE_CHANNEL, "EXPLORE_CHANNEL", channelCode, operator, now,
                null, TraceContext.currentTraceId(), "status=" + status.name()));
        log.info("探索页频道维护成功: channelCode={}, status={}, operator={}", channelCode, status, operator);
        return channelRepository.findByCode(channelCode).orElse(updated);
    }
}
