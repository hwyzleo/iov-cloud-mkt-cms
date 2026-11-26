package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExplorePageList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageComponentMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageCurrentMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageItemMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageComponentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageCurrentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageItemPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePagePo;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 探索页频道页面仓储实现。
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class ExplorePageRepositoryImpl implements ExplorePageRepository {

    private final ExplorePageMapper pageMapper;
    private final ExplorePageComponentMapper componentMapper;
    private final ExplorePageItemMapper itemMapper;
    private final ExplorePageCurrentMapper currentMapper;

    @Override
    public void insert(ExplorePage page) {
        pageMapper.insert(ExploreConverter.fromDomain(page));
    }

    @Override
    public int updateDraft(ExplorePage page, int expectedLockVersion) {
        UpdateWrapper<ExplorePagePo> wrapper = new UpdateWrapper<>();
        wrapper.eq("page_id", page.getPageId())
                .eq("page_version", 0)
                .eq("lock_version", expectedLockVersion)
                .set("lock_version", page.getLockVersion())
                .set("modify_by", page.getModifyBy())
                .set("modify_time", page.getModifyTime());
        return pageMapper.update(null, wrapper);
    }

    @Override
    public int updateStatus(ExplorePage page) {
        UpdateWrapper<ExplorePagePo> wrapper = new UpdateWrapper<>();
        wrapper.eq("page_id", page.getPageId())
                .set("status", page.getStatus() == null ? null : page.getStatus().name())
                .set("published_at", page.getPublishedAt())
                .set("offline_at", page.getOfflineAt())
                .set("modify_by", page.getModifyBy())
                .set("modify_time", page.getModifyTime());
        return pageMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExplorePage> findById(String pageId) {
        return Optional.ofNullable(pageMapper.selectOne(new QueryWrapper<ExplorePagePo>()
                        .eq("page_id", pageId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExplorePage> findByIdForUpdate(String pageId) {
        return Optional.ofNullable(pageMapper.selectOne(new QueryWrapper<ExplorePagePo>()
                        .eq("page_id", pageId)
                        .last("FOR UPDATE")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExplorePage> findDraft(String channelCode) {
        return Optional.ofNullable(pageMapper.selectOne(new QueryWrapper<ExplorePagePo>()
                        .eq("channel_code", channelCode)
                        .eq("page_version", 0)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExplorePage> findDraftForUpdate(String channelCode) {
        return Optional.ofNullable(pageMapper.selectOne(new QueryWrapper<ExplorePagePo>()
                        .eq("channel_code", channelCode)
                        .eq("page_version", 0)
                        .last("FOR UPDATE")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExplorePage> findByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pageMapper.selectOne(new QueryWrapper<ExplorePagePo>()
                        .eq("request_id", requestId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExplorePage> findScheduled(String channelCode) {
        return Optional.ofNullable(pageMapper.selectOne(new QueryWrapper<ExplorePagePo>()
                        .eq("channel_code", channelCode)
                        .eq("status", ExplorePageStatus.SCHEDULED.name())
                        .orderByDesc("page_version")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExplorePage> findCurrentPage(String channelCode) {
        ExplorePageCurrentPo current = currentMapper.selectByChannel(channelCode);
        if (current == null || !StringUtils.hasText(current.getReleaseId())) {
            return Optional.empty();
        }
        return findById(current.getReleaseId());
    }

    @Override
    public int maxPageVersion(String channelCode) {
        ExplorePagePo po = pageMapper.selectOne(new QueryWrapper<ExplorePagePo>()
                .select("MAX(page_version) AS page_version")
                .eq("channel_code", channelCode));
        return po == null || po.getPageVersion() == null ? 0 : po.getPageVersion();
    }

    @Override
    public List<ExplorePage> searchReleases(ExplorePageSearchCriteria criteria) {
        QueryWrapper<ExplorePagePo> wrapper = new QueryWrapper<>();
        wrapper.gt("page_version", 0);
        if (StringUtils.hasText(criteria.getChannelCode())) {
            wrapper.eq("channel_code", criteria.getChannelCode().trim());
        }
        if (StringUtils.hasText(criteria.getStatus())) {
            wrapper.eq("status", criteria.getStatus().trim());
        }
        wrapper.orderByDesc("page_version");
        return ExplorePageList.map(pageMapper.selectList(wrapper), ExploreConverter::toDomain);
    }

    @Override
    public void deletePageContent(String pageId) {
        itemMapper.delete(new QueryWrapper<ExplorePageItemPo>()
                .apply("component_id IN (SELECT component_id FROM cms_explore_page_component"
                        + " WHERE page_id = {0})", pageId));
        componentMapper.delete(new QueryWrapper<ExplorePageComponentPo>().eq("page_id", pageId));
    }

    @Override
    public void insertComponent(ExplorePageComponent component) {
        componentMapper.insert(ExploreConverter.fromDomain(component));
    }

    @Override
    public void insertItem(ExplorePageItem item) {
        itemMapper.insert(ExploreConverter.fromDomain(item));
    }

    @Override
    public List<ExplorePageComponent> listComponents(String pageId) {
        return componentMapper.selectList(new QueryWrapper<ExplorePageComponentPo>()
                        .eq("page_id", pageId)
                        .orderByAsc("sort_no")).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExplorePageItem> listItems(String componentId) {
        return itemMapper.selectList(new QueryWrapper<ExplorePageItemPo>()
                        .eq("component_id", componentId)
                        .orderByAsc("sort_no", "item_id")).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExplorePageItem> listItemsByComponentIds(List<String> componentIds) {
        if (componentIds == null || componentIds.isEmpty()) {
            return List.of();
        }
        return itemMapper.selectList(new QueryWrapper<ExplorePageItemPo>()
                        .in("component_id", componentIds)
                        .orderByAsc("sort_no", "item_id")).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExplorePageItem> feedPage(String componentId, Integer lastSortNo,
                                          String lastItemId, int limit) {
        QueryWrapper<ExplorePageItemPo> wrapper = new QueryWrapper<>();
        wrapper.eq("component_id", componentId);
        if (lastSortNo != null && StringUtils.hasText(lastItemId)) {
            wrapper.apply("(sort_no > {0} OR (sort_no = {0} AND item_id > {1}))", lastSortNo, lastItemId);
        }
        wrapper.orderByAsc("sort_no", "item_id").last("LIMIT " + limit);
        return itemMapper.selectList(wrapper).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExplorePage> findDueSchedules(java.time.Instant now, int limit) {
        return pageMapper.selectList(new QueryWrapper<ExplorePagePo>()
                        .eq("status", ExplorePageStatus.SCHEDULED.name())
                        .le("effective_at", now)
                        .orderByAsc("effective_at")
                        .last("LIMIT " + limit)).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExplorePage> findExpiredCurrent(java.time.Instant now, int limit) {
        List<ExplorePageCurrentPo> currents = currentMapper.selectAll();
        List<String> releaseIds = currents.stream()
                .map(ExplorePageCurrentPo::getReleaseId)
                .filter(releaseId -> releaseId != null && !releaseId.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (releaseIds.isEmpty()) {
            return List.of();
        }
        return pageMapper.selectList(new QueryWrapper<ExplorePagePo>()
                        .in("page_id", releaseIds)
                        .eq("status", ExplorePageStatus.PUBLISHED.name())
                        .le("expire_at", now)
                        .last("LIMIT " + limit)).stream()
                .map(ExploreConverter::toDomain)
                .collect(Collectors.toList());
    }
}
