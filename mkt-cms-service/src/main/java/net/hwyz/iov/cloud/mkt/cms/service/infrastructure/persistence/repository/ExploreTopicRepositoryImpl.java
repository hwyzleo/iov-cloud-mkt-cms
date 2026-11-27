package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExplorePageList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreContentSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreTopicImageMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreTopicItemMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreTopicMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreTopicRevisionMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreTopicImagePo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreTopicItemPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreTopicPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreTopicRevisionPo;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 探索页专题仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ExploreTopicRepositoryImpl implements ExploreTopicRepository {

    private final ExploreTopicMapper topicMapper;
    private final ExploreTopicRevisionMapper revisionMapper;
    private final ExploreTopicItemMapper itemMapper;
    private final ExploreTopicImageMapper imageMapper;

    @Override
    public void insert(ExploreTopic topic) {
        topicMapper.insert(ExploreConverter.fromDomain(topic));
    }

    @Override
    public int update(ExploreTopic topic, int expectedLockVersion) {
        UpdateWrapper<ExploreTopicPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("topic_id", topic.getTopicId())
                .eq("lock_version", expectedLockVersion)
                .set("status", topic.getStatus() == null ? null : topic.getStatus().name())
                .set("current_revision_id", topic.getCurrentRevisionId())
                .set("lock_version", topic.getLockVersion())
                .set("modify_by", topic.getModifyBy())
                .set("modify_time", topic.getModifyTime());
        return topicMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExploreTopic> findByTopicId(String topicId) {
        return Optional.ofNullable(topicMapper.selectOne(new QueryWrapper<ExploreTopicPo>()
                        .eq("topic_id", topicId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreTopic> findByTopicIdForUpdate(String topicId) {
        return Optional.ofNullable(topicMapper.selectOne(new QueryWrapper<ExploreTopicPo>()
                        .eq("topic_id", topicId)
                        .last("FOR UPDATE")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public List<ExploreTopic> search(ExploreContentSearchCriteria criteria) {
        QueryWrapper<ExploreTopicPo> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(criteria.getStatus())) {
            wrapper.eq("status", criteria.getStatus().trim());
        }
        if (StringUtils.hasText(criteria.getKeyword())) {
            wrapper.apply("EXISTS (SELECT 1 FROM cms_explore_topic_revision r"
                            + " WHERE r.topic_id = cms_explore_topic.topic_id"
                            + " AND r.name LIKE CONCAT('%', {0}, '%'))", criteria.getKeyword().trim());
        }
        wrapper.orderByDesc("create_time");
        return ExplorePageList.map(topicMapper.selectList(wrapper), ExploreConverter::toDomain);
    }

    @Override
    public void insertRevision(ExploreTopicRevision revision) {
        revisionMapper.insert(ExploreConverter.fromDomain(revision));
    }

    @Override
    public int updateDraftRevision(ExploreTopicRevision revision) {
        ExploreTopicRevisionPo po = ExploreConverter.fromDomain(revision);
        UpdateWrapper<ExploreTopicRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("status", ExploreRevisionStatus.DRAFT.name())
                .set("name", po.getName())
                .set("summary", po.getSummary())
                .set("cover_url", po.getCoverUrl())
                .set("cover_width", po.getCoverWidth())
                .set("cover_height", po.getCoverHeight())
                .set("cover_mime", po.getCoverMime())
                .set("cover_size", po.getCoverSize())
                .set("target_type", po.getTargetType())
                .set("target_ref_id", po.getTargetRefId())
                .set("target_revision_id", po.getTargetRevisionId())
                .set("target_url", po.getTargetUrl())
                .set("target_route_code", po.getTargetRouteCode())
                .set("target_params", po.getTargetParams())
                .set("content_hash", po.getContentHash())
                .set("modify_by", po.getModifyBy())
                .set("modify_time", po.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public int publishRevision(ExploreTopicRevision revision) {
        UpdateWrapper<ExploreTopicRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("status", ExploreRevisionStatus.DRAFT.name())
                .set("status", ExploreRevisionStatus.PUBLISHED.name())
                .set("published_at", revision.getPublishedAt())
                .set("modify_by", revision.getModifyBy())
                .set("modify_time", revision.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExploreTopicRevision> findRevision(String revisionId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreTopicRevisionPo>()
                        .eq("revision_id", revisionId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreTopicRevision> findLatestDraft(String topicId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreTopicRevisionPo>()
                        .eq("topic_id", topicId)
                        .eq("status", ExploreRevisionStatus.DRAFT.name())
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreTopicRevision> findLatestRevision(String topicId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreTopicRevisionPo>()
                        .eq("topic_id", topicId)
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public void replaceItems(String topicRevisionId, List<ExploreTopicItem> items) {
        itemMapper.delete(new QueryWrapper<ExploreTopicItemPo>().eq("topic_revision_id", topicRevisionId));
        if (items == null) {
            return;
        }
        for (ExploreTopicItem item : items) {
            itemMapper.insert(ExploreConverter.fromDomain(item));
        }
    }

    @Override
    public List<ExploreTopicItem> listItems(String topicRevisionId) {
        return itemMapper.selectList(new QueryWrapper<ExploreTopicItemPo>()
                        .eq("topic_revision_id", topicRevisionId)
                        .orderByAsc("sort_no")).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExploreTopicItem> listItemsAfter(String topicRevisionId, Integer lastSortNo, int limit) {
        QueryWrapper<ExploreTopicItemPo> wrapper = new QueryWrapper<>();
        wrapper.eq("topic_revision_id", topicRevisionId);
        if (lastSortNo != null) {
            wrapper.gt("sort_no", lastSortNo);
        }
        wrapper.orderByAsc("sort_no").last("LIMIT " + limit);
        return itemMapper.selectList(wrapper).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExploreTopic> findByIds(java.util.Collection<String> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return List.of();
        }
        return topicMapper.selectList(new QueryWrapper<ExploreTopicPo>()
                        .in("topic_id", topicIds)).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void replaceRevisionImages(String topicRevisionId, List<ExploreImage> images) {
        imageMapper.delete(new QueryWrapper<ExploreTopicImagePo>()
                .eq("topic_revision_id", topicRevisionId));
        if (images == null || images.isEmpty()) {
            return;
        }
        int sortNo = 10;
        for (ExploreImage image : images) {
            ExploreTopicImagePo po = new ExploreTopicImagePo();
            po.setTopicRevisionId(topicRevisionId);
            po.setSortNo(sortNo);
            po.setUrl(image.getUrl());
            po.setWidth(image.getWidth());
            po.setHeight(image.getHeight());
            po.setMime(image.getMime());
            po.setSize(image.getSize());
            po.setShowInDetail(image.detailVisible());
            imageMapper.insert(po);
            sortNo += 10;
        }
    }

    @Override
    public List<ExploreImage> findRevisionImages(String topicRevisionId) {
        return imageMapper.selectList(new QueryWrapper<ExploreTopicImagePo>()
                        .eq("topic_revision_id", topicRevisionId)
                        .orderByAsc("sort_no")).stream()
                .map(ExploreTopicRepositoryImpl::image)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<ExploreImage>> findRevisionImagesByIds(Collection<String> topicRevisionIds) {
        if (topicRevisionIds == null || topicRevisionIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<ExploreImage>> result = new LinkedHashMap<>();
        for (ExploreTopicImagePo po : imageMapper.selectList(new QueryWrapper<ExploreTopicImagePo>()
                .in("topic_revision_id", topicRevisionIds)
                .orderByAsc("topic_revision_id", "sort_no"))) {
            result.computeIfAbsent(po.getTopicRevisionId(), key -> new java.util.ArrayList<>())
                    .add(image(po));
        }
        return result;
    }

    private static ExploreImage image(ExploreTopicImagePo po) {
        return ExploreImage.builder()
                .url(po.getUrl())
                .width(po.getWidth())
                .height(po.getHeight())
                .mime(po.getMime())
                .size(po.getSize())
                .showInDetail(po.getShowInDetail() == null || po.getShowInDetail())
                .build();
    }
}
