package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBannerRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreContentSearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * 探索页 Banner 仓储（根实体与不可变修订）。
 *
 * @author hwyz_leo
 */
public interface ExploreBannerRepository {

    void insert(ExploreBanner banner);

    int update(ExploreBanner banner, int expectedLockVersion);

    Optional<ExploreBanner> findByBannerId(String bannerId);

    Optional<ExploreBanner> findByBannerIdForUpdate(String bannerId);

    List<ExploreBanner> search(ExploreContentSearchCriteria criteria);

    void insertRevision(ExploreBannerRevision revision);

    int updateDraftRevision(ExploreBannerRevision revision);

    int publishRevision(ExploreBannerRevision revision);

    /**
     * 批量查询 Banner 根实体
     *
     * @param bannerIds Banner 标识集合
     * @return Banner 列表
     */
    List<ExploreBanner> findByIds(java.util.Collection<String> bannerIds);

    Optional<ExploreBannerRevision> findRevision(String revisionId);

    Optional<ExploreBannerRevision> findLatestDraft(String bannerId);

    Optional<ExploreBannerRevision> findLatestRevision(String bannerId);
}
