package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreChannelMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageCurrentMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreChannelPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageCurrentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePagePo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 探索页频道仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ExploreChannelRepositoryImpl implements ExploreChannelRepository {

    private final ExploreChannelMapper channelMapper;
    private final ExplorePageCurrentMapper currentMapper;
    private final ExplorePageMapper pageMapper;

    @Override
    public List<ExploreChannel> findAll() {
        return channelMapper.selectList(new QueryWrapper<ExploreChannelPo>()
                        .orderByAsc("sort_no", "channel_code")).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ExploreChannel> findByCode(String channelCode) {
        return Optional.ofNullable(channelMapper.selectOne(new QueryWrapper<ExploreChannelPo>()
                        .eq("channel_code", channelCode)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public List<ExploreChannel> findEnabledWithCurrent(Instant now) {
        List<ExplorePageCurrentPo> currents = currentMapper.selectAll();
        List<String> releaseIds = currents.stream()
                .map(ExplorePageCurrentPo::getReleaseId)
                .filter(releaseId -> releaseId != null && !releaseId.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (releaseIds.isEmpty()) {
            return List.of();
        }
        Map<String, ExplorePagePo> currentPages = pageMapper.selectList(new QueryWrapper<ExplorePagePo>()
                        .in("page_id", releaseIds))
                .stream()
                .filter(page -> ExplorePageStatus.PUBLISHED.name().equals(page.getStatus()))
                .filter(page -> page.getEffectiveAt() == null || !page.getEffectiveAt().isAfter(now))
                .filter(page -> page.getExpireAt() == null || page.getExpireAt().isAfter(now))
                .collect(Collectors.toMap(ExplorePagePo::getPageId, page -> page, (left, right) -> left,
                        LinkedHashMap::new));
        if (currentPages.isEmpty()) {
            return List.of();
        }
        Map<String, String> channelRelease = new LinkedHashMap<>();
        for (ExplorePageCurrentPo current : currents) {
            if (current.getReleaseId() != null && currentPages.containsKey(current.getReleaseId())) {
                channelRelease.put(current.getChannelCode(), current.getReleaseId());
            }
        }
        if (channelRelease.isEmpty()) {
            return List.of();
        }
        return channelMapper.selectList(new QueryWrapper<ExploreChannelPo>()
                        .eq("status", ExploreChannelStatus.ENABLED.name())
                        .in("channel_code", channelRelease.keySet())
                        .orderByAsc("sort_no", "channel_code")).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int update(ExploreChannel channel, int expectedLockVersion) {
        UpdateWrapper<ExploreChannelPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("channel_code", channel.getChannelCode())
                .eq("lock_version", expectedLockVersion)
                .set("name", channel.getName())
                .set("sort_no", channel.getSortNo())
                .set("status", channel.getStatus() == null ? null : channel.getStatus().name())
                .set("lock_version", channel.getLockVersion())
                .set("modify_by", channel.getModifyBy())
                .set("modify_time", channel.getModifyTime());
        return channelMapper.update(null, wrapper);
    }
}
