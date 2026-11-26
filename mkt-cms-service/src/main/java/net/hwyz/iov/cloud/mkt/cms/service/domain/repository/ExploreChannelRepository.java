package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 探索页频道仓储。
 *
 * @author hwyz_leo
 */
public interface ExploreChannelRepository {

    /**
     * 全部频道（按 sort_no + channel_code）
     *
     * @return 频道列表
     */
    List<ExploreChannel> findAll();

    /**
     * 按编码查询
     *
     * @param channelCode 频道编码
     * @return 频道
     */
    Optional<ExploreChannel> findByCode(String channelCode);

    /**
     * 已启用且存在当前有效页面的频道
     *
     * @param now 服务端时间
     * @return 频道列表
     */
    List<ExploreChannel> findEnabledWithCurrent(Instant now);

    /**
     * 按乐观锁更新频道
     *
     * @param channel             频道
     * @param expectedLockVersion 期望乐观锁版本
     * @return 影响行数
     */
    int update(ExploreChannel channel, int expectedLockVersion);
}
