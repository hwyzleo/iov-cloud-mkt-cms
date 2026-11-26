package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageCurrentRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageCurrentMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 探索页频道当前版本指针仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ExplorePageCurrentRepositoryImpl implements ExplorePageCurrentRepository {

    private final ExplorePageCurrentMapper currentMapper;

    @Override
    public Optional<ExplorePageCurrent> find(String channelCode) {
        return Optional.ofNullable(currentMapper.selectByChannel(channelCode))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExplorePageCurrent> lock(String channelCode) {
        return Optional.ofNullable(currentMapper.selectByChannelForUpdate(channelCode))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public void insertEmpty(String channelCode, Instant now) {
        currentMapper.insertEmpty(channelCode, now);
    }

    @Override
    public int switchPointer(String channelCode, int expectedLockVersion, String releaseId, Instant now) {
        return currentMapper.switchPointer(channelCode, expectedLockVersion, releaseId, now);
    }

    @Override
    public int clearPointer(String channelCode, int expectedLockVersion, Instant now) {
        return currentMapper.clearPointer(channelCode, expectedLockVersion, now);
    }

    @Override
    public int activate(String channelCode, String expectedReleaseId, String newReleaseId, Instant now) {
        return currentMapper.activate(channelCode, expectedReleaseId, newReleaseId, now);
    }

    @Override
    public int expire(String channelCode, String expectedReleaseId, Instant now) {
        return currentMapper.expire(channelCode, expectedReleaseId, now);
    }
}
