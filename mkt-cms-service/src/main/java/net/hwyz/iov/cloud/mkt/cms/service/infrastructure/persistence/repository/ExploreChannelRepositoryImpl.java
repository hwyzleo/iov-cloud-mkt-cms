package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.converter.ExploreConverter;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreChannelMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExploreChannelRevisionMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageCurrentMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper.ExplorePageMapper;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreChannelPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExploreChannelRevisionPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageCurrentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePagePo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final ExploreChannelRevisionMapper revisionMapper;
    private final ExplorePageCurrentMapper currentMapper;
    private final ExplorePageMapper pageMapper;

    @Override
    public List<ExploreChannel> findAll() {
        return channelMapper.selectList(new QueryWrapper<ExploreChannelPo>()
                        .orderByAsc("channel_code")).stream()
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
    public Optional<ExploreChannel> findByCodeForUpdate(String channelCode) {
        return Optional.ofNullable(channelMapper.selectOne(new QueryWrapper<ExploreChannelPo>()
                        .eq("channel_code", channelCode)
                        .last("FOR UPDATE")))
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
        List<ExploreChannelPo> roots = channelMapper.selectList(new QueryWrapper<ExploreChannelPo>()
                .isNotNull("current_revision_id")
                .in("channel_code", channelRelease.keySet()));
        if (roots.isEmpty()) {
            return List.of();
        }
        List<String> revisionIds = roots.stream()
                .map(ExploreChannelPo::getCurrentRevisionId)
                .filter(revisionId -> revisionId != null && !revisionId.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (revisionIds.isEmpty()) {
            return List.of();
        }
        Map<String, ExploreChannelRevisionPo> revisions = new LinkedHashMap<>();
        for (ExploreChannelRevisionPo po : revisionMapper.selectList(new QueryWrapper<ExploreChannelRevisionPo>()
                .in("revision_id", revisionIds))) {
            revisions.put(po.getRevisionId(), po);
        }
        List<ExploreChannel> visible = new ArrayList<>();
        for (ExploreChannelPo root : roots) {
            if (root.getCurrentRevisionId() == null) {
                continue;
            }
            ExploreChannelRevisionPo revision = revisions.get(root.getCurrentRevisionId());
            if (revision == null
                    || !ExploreRevisionStatus.PUBLISHED.name().equals(revision.getRevisionStatus())
                    || !ExploreChannelStatus.ENABLED.name().equals(revision.getStatus())) {
                continue;
            }
            visible.add(ExploreConverter.toDomain(root).toBuilder()
                    .name(revision.getName())
                    .sortNo(revision.getSortNo() == null ? 0 : revision.getSortNo())
                    .status(ExploreChannelStatus.ENABLED)
                    .build());
        }
        visible.sort(Comparator.comparingInt(ExploreChannel::getSortNo)
                .thenComparing(ExploreChannel::getChannelCode));
        return visible;
    }

    @Override
    public void insert(ExploreChannel channel) {
        channelMapper.insert(ExploreConverter.fromDomain(channel));
    }

    @Override
    public int update(ExploreChannel channel, int expectedLockVersion) {
        UpdateWrapper<ExploreChannelPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("channel_code", channel.getChannelCode())
                .eq("lock_version", expectedLockVersion)
                .set("name", channel.getName())
                .set("sort_no", channel.getSortNo())
                .set("status", channel.getStatus() == null ? null : channel.getStatus().name())
                .set("draft_revision_id", channel.getDraftRevisionId())
                .set("current_revision_id", channel.getCurrentRevisionId())
                .set("lock_version", channel.getLockVersion())
                .set("modify_by", channel.getModifyBy())
                .set("modify_time", channel.getModifyTime());
        return channelMapper.update(null, wrapper);
    }

    @Override
    public int deleteByCode(String channelCode, int expectedLockVersion) {
        return channelMapper.delete(new QueryWrapper<ExploreChannelPo>()
                .eq("channel_code", channelCode)
                .eq("lock_version", expectedLockVersion));
    }

    @Override
    public void deleteRevisions(String channelCode) {
        revisionMapper.delete(new QueryWrapper<ExploreChannelRevisionPo>()
                .eq("channel_code", channelCode));
    }

    @Override
    public void insertRevision(ExploreChannelRevision revision) {
        revisionMapper.insert(ExploreConverter.fromDomain(revision));
    }

    @Override
    public int updateDraftRevision(ExploreChannelRevision revision) {
        ExploreChannelRevisionPo po = ExploreConverter.fromDomain(revision);
        UpdateWrapper<ExploreChannelRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("revision_status", ExploreRevisionStatus.DRAFT.name())
                .set("name", po.getName())
                .set("sort_no", po.getSortNo())
                .set("status", po.getStatus())
                .set("content_hash", po.getContentHash())
                .set("modify_by", po.getModifyBy())
                .set("modify_time", po.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public int publishRevision(ExploreChannelRevision revision) {
        ExploreChannelRevisionPo po = ExploreConverter.fromDomain(revision);
        UpdateWrapper<ExploreChannelRevisionPo> wrapper = new UpdateWrapper<>();
        wrapper.eq("revision_id", revision.getRevisionId())
                .eq("revision_status", ExploreRevisionStatus.DRAFT.name())
                .set("revision_status", ExploreRevisionStatus.PUBLISHED.name())
                .set("published_by", po.getPublishedBy())
                .set("published_at", po.getPublishedAt())
                .set("request_id", po.getRequestId())
                .set("request_hash", po.getRequestHash())
                .set("expected_current_revision_id", po.getExpectedCurrentRevisionId())
                .set("modify_by", po.getModifyBy())
                .set("modify_time", po.getModifyTime());
        return revisionMapper.update(null, wrapper);
    }

    @Override
    public Optional<ExploreChannelRevision> findRevision(String revisionId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreChannelRevisionPo>()
                        .eq("revision_id", revisionId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Map<String, ExploreChannelRevision> findRevisionsByIds(Collection<String> revisionIds) {
        if (revisionIds == null || revisionIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ExploreChannelRevision> result = new LinkedHashMap<>();
        for (ExploreChannelRevisionPo po : revisionMapper.selectList(new QueryWrapper<ExploreChannelRevisionPo>()
                .in("revision_id", revisionIds))) {
            result.put(po.getRevisionId(), ExploreConverter.toDomain(po));
        }
        return result;
    }

    @Override
    public Optional<ExploreChannelRevision> findLatestRevision(String channelCode) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreChannelRevisionPo>()
                        .eq("channel_code", channelCode)
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreChannelRevision> findLatestDraft(String channelCode) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreChannelRevisionPo>()
                        .eq("channel_code", channelCode)
                        .eq("revision_status", ExploreRevisionStatus.DRAFT.name())
                        .orderByDesc("revision_no")
                        .last("LIMIT 1")))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public Optional<ExploreChannelRevision> findCurrentPublishedRevision(String channelCode) {
        ExploreChannelPo root = channelMapper.selectOne(new QueryWrapper<ExploreChannelPo>()
                .eq("channel_code", channelCode));
        if (root == null || root.getCurrentRevisionId() == null) {
            return Optional.empty();
        }
        return findRevision(root.getCurrentRevisionId())
                .filter(revision -> revision.getRevisionStatus() == ExploreRevisionStatus.PUBLISHED);
    }

    @Override
    public Optional<ExploreChannelRevision> findByRequestId(String requestId) {
        return Optional.ofNullable(revisionMapper.selectOne(new QueryWrapper<ExploreChannelRevisionPo>()
                        .eq("request_id", requestId)))
                .map(ExploreConverter::toDomain);
    }

    @Override
    public boolean existsPageReference(String channelCode) {
        Long count = pageMapper.selectCount(new QueryWrapper<ExplorePagePo>()
                .eq("channel_code", channelCode));
        return count != null && count > 0;
    }

    @Override
    public Set<String> findReferencedChannelCodes(Collection<String> channelCodes) {
        if (channelCodes == null || channelCodes.isEmpty()) {
            return Set.of();
        }
        return pageMapper.selectList(new QueryWrapper<ExplorePagePo>()
                        .select("channel_code")
                        .in("channel_code", channelCodes)).stream()
                .map(ExplorePagePo::getChannelCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
