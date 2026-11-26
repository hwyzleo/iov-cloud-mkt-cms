package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageCurrent;

import java.time.Instant;
import java.util.Optional;

/**
 * 探索页频道当前版本指针仓储。
 *
 * @author hwyz_leo
 */
public interface ExplorePageCurrentRepository {

    Optional<ExplorePageCurrent> find(String channelCode);

    Optional<ExplorePageCurrent> lock(String channelCode);

    void insertEmpty(String channelCode, Instant now);

    int switchPointer(String channelCode, int expectedLockVersion, String releaseId, Instant now);

    int clearPointer(String channelCode, int expectedLockVersion, Instant now);

    int activate(String channelCode, String expectedReleaseId, String newReleaseId, Instant now);

    int expire(String channelCode, String expectedReleaseId, Instant now);
}
