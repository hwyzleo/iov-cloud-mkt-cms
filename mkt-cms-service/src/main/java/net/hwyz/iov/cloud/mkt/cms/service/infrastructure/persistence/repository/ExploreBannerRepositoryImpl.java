package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExplorePageList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBannerRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreContentSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreBannerRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreBannerMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreBannerRevisionMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreBannerPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreBannerRevisionPo;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 探索页 Banner 仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ExploreBannerRepositoryImpl implements ExploreBannerRepository {

    private final ExploreBannerMapper bannerMapper;
    private final ExploreBannerRevisionMapper revisionMapper;

    @Override
    public void insert(ExploreBanner banner) {
        bannerMapper.insert(ExploreConverter.fromDomain(banner));
    }

    @Override
    public int update(ExploreBanner banner, int expectedLockVersion) {
        UpdateWrapper<ExploreBannerPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("banner_id", banner.getBannerId())
                .eq("lock_version", expectedLockVersion)
                .set("status", banner.getStatus() == null ? null : banner.getStatus().name())
                .set("current_revision_id", banner.getCurrentRevisionId())
                .set("lock_version", banner.getLockVersion())
                .set("modify_by", banner.getModifyBy())
                .set("modify_time", banner.getModifyTime());
        return bannerMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExploreBanner> findByBannerId(String bannerId) {
        return Optional.ofNullable(bannerMapper.selectOne(new QueryWrapper<ExploreBannerPo>()
                        .eq("banner_id", bannerId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreBanner> findByBannerIdForUpdate(String bannerId) {
        return Optional.ofNullable(bannerMapper.selectOne(new QueryWrapper<ExploreBannerPo>()
                        .eq("banner_id", bannerId)
                        .last("FOR UPDATE")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public List<ExploreBanner> search(ExploreContentSearchCriteria criteria) {
        QueryWrapper<ExploreBannerPo> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(criteria.getStatus())) {
            wrapper.eq("status", criteria.getStatus().trim());
        }
        if (StringUtils.hasText(criteria.getKeyword())) {
            wrapper.apply("EXISTS (SELECT 1 FROM cms_explore_banner_revision r"
                            + " WHERE r.banner_id = cms_explore_banner.banner_id"
                            + " AND r.title LIKE CONCAT('%', {0}, '%'))", criteria.getKeyword().trim());
        }
        wrapper.orderByDesc("create_time");
        return ExplorePageList.map(bannerMapper.selectList(wrapper), ExploreConverter::toDomain);
    }

    @Override
    public void insertRevision(ExploreBannerRevision revision) {
        revisionMapper.insert(ExploreConverter.fromDomain(revision));
    }

    @Override
    public int updateDraftRevision(ExploreBannerRevision revision) {
        ExploreBannerRevisionPo po = ExploreConverter.fromDomain(revision);
        UpdateWrapper<ExploreBannerRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("status", ExploreRevisionStatus.DRAFT.name())
                .set("title", po.getTitle())
                .set("image_url", po.getImageUrl())
                .set("image_width", po.getImageWidth())
                .set("image_height", po.getImageHeight())
                .set("image_mime", po.getImageMime())
                .set("image_size", po.getImageSize())
                .set("target_type", po.getTargetType())
                .set("target_ref_id", po.getTargetRefId())
                .set("target_revision_id", po.getTargetRevisionId())
                .set("target_url", po.getTargetUrl())
                .set("target_route_code", po.getTargetRouteCode())
                .set("target_params", po.getTargetParams())
                .set("default_sort_no", po.getDefaultSortNo())
                .set("content_hash", po.getContentHash())
                .set("modify_by", po.getModifyBy())
                .set("modify_time", po.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public int publishRevision(ExploreBannerRevision revision) {
        UpdateWrapper<ExploreBannerRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("status", ExploreRevisionStatus.DRAFT.name())
                .set("status", ExploreRevisionStatus.PUBLISHED.name())
                .set("published_at", revision.getPublishedAt())
                .set("modify_by", revision.getModifyBy())
                .set("modify_time", revision.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExploreBannerRevision> findRevision(String revisionId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreBannerRevisionPo>()
                        .eq("revision_id", revisionId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreBannerRevision> findLatestDraft(String bannerId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreBannerRevisionPo>()
                        .eq("banner_id", bannerId)
                        .eq("status", ExploreRevisionStatus.DRAFT.name())
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreBannerRevision> findLatestRevision(String bannerId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreBannerRevisionPo>()
                        .eq("banner_id", bannerId)
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public List<ExploreBanner> findByIds(java.util.Collection<String> bannerIds) {
        if (bannerIds == null || bannerIds.isEmpty()) {
            return List.of();
        }
        return bannerMapper.selectList(new QueryWrapper<ExploreBannerPo>()
                        .in("banner_id", bannerIds)).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }
}
