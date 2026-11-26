package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreItemType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreSourceType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreComponentView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreCursor;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreBannerRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreCursorCodec;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreImageLayout;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config.ExploreProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 探索页手机端只读查询应用服务。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.2 / D-014：仅返回当前有效发布版本与已发布、未下线内容；
 * 信息流使用同一发布版本内的 keyset 物理分页；无当前页面返回 105011；
 * 翻页 releaseId 不再是当前版本返回 105017。
 *
 * @author hwyz_leo
 */
@Service
@RequiredArgsConstructor
public class ExploreMobileQueryAppService {

    private final ExploreChannelRepository channelRepository;
    private final ExplorePageRepository pageRepository;
    private final ExploreArticleRepository articleRepository;
    private final ExploreTopicRepository topicRepository;
    private final ExploreBannerRepository bannerRepository;
    private final ExplorePageContentService contentService;
    private final ExploreCursorCodec cursorCodec;
    private final ExploreProperties properties;
    private final Clock clock;

    /**
     * 已启用且有当前有效页面的频道
     *
     * @return 频道列表
     */
    public List<Map<String, Object>> channels() {
        Instant now = Instant.now(clock);
        return channelRepository.findEnabledWithCurrent(now).stream()
                .map(this::channelView)
                .collect(Collectors.toList());
    }

    /**
     * 频道当前页面（含信息流首页）
     *
     * @param channelCode 频道编码
     * @param pageSize    信息流页大小
     * @return 页面视图
     */
    public ExplorePageView currentPage(String channelCode, Integer pageSize) {
        Instant now = Instant.now(clock);
        int size = normalizePageSize(pageSize);
        ExploreChannel channel = requireMobileChannel(channelCode);
        ExplorePage page = requireCurrentPage(channel.getChannelCode(), now);
        List<ExploreComponentView> components = new ArrayList<>();
        for (ExploreComponentData data : contentService.load(page.getPageId())) {
            ExploreComponentType type = data.getComponent().getComponentType();
            if (type == ExploreComponentType.ARTICLE_FEED) {
                components.add(buildFeedComponent(data, size, now));
            } else {
                List<Map<String, Object>> items = visibleItems(data.getItems(), now).stream()
                        .map(ExplorePageItem::displayView).toList();
                components.add(ExploreComponentView.builder()
                        .type(type.name())
                        .componentId(data.getComponent().getComponentId())
                        .sortNo(data.getComponent().getSortNo())
                        .config(componentConfig(type, data.getComponent().configView()))
                        .items(new ArrayList<>(items))
                        .hasMore(false)
                        .build());
            }
        }
        return pageView(channel, page, components, false);
    }

    /**
     * 信息流翻页（同一发布版本 keyset）
     *
     * @param channelCode 频道编码
     * @param releaseId   首页返回的发布版本
     * @param cursor      不透明游标
     * @param pageSize    页大小
     * @return 信息流分页
     */
    public Map<String, Object> articleFeed(String channelCode, String releaseId, String cursor,
                                           Integer pageSize) {
        Instant now = Instant.now(clock);
        int size = normalizePageSize(pageSize);
        ExploreChannel channel = requireMobileChannel(channelCode);
        ExplorePage page = requireCurrentPage(channel.getChannelCode(), now);
        if (!page.getPageId().equals(releaseId)) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_RELEASE_CHANGED,
                    "发布版本已变化，请刷新探索页首页");
        }
        ExploreComponentData feed = null;
        for (ExploreComponentData data : contentService.load(page.getPageId())) {
            if (data.getComponent().getComponentType() == ExploreComponentType.ARTICLE_FEED) {
                feed = data;
                break;
            }
        }
        if (feed == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("releaseId", releaseId);
            empty.put("items", List.of());
            empty.put("hasMore", false);
            return empty;
        }
        Integer lastSortNo = null;
        String lastItemId = null;
        if (StringUtils.hasText(cursor)) {
            ExploreCursor decoded = cursorCodec.decode(cursor);
            if (!releaseId.equals(decoded.getReleaseId())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_RELEASE_CHANGED,
                        "游标所属发布版本已变化，请刷新探索页首页");
            }
            if (!feed.getComponent().getComponentId().equals(decoded.getComponentId())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "游标组件不匹配");
            }
            lastSortNo = decoded.getSortNo();
            lastItemId = decoded.getItemId();
        }
        List<ExplorePageItem> raw = pageRepository.feedPage(feed.getComponent().getComponentId(),
                lastSortNo, lastItemId, size + 1);
        boolean hasMore = raw.size() > size;
        List<ExplorePageItem> pageItems = hasMore ? raw.subList(0, size) : raw;
        List<ExplorePageItem> visible = visibleItems(pageItems, now);
        String nextCursor = hasMore
                ? cursorCodec.encode(releaseId, feed.getComponent().getComponentId(),
                raw.get(size - 1).getSortNo(), raw.get(size - 1).getItemId())
                : null;
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("releaseId", releaseId);
        view.put("componentId", feed.getComponent().getComponentId());
        view.put("items", visible.stream().map(ExplorePageItem::displayView).toList());
        view.put("hasMore", hasMore);
        if (nextCursor != null) {
            view.put("nextCursor", nextCursor);
        }
        return view;
    }

    /**
     * 文章详情（仅已发布且未下线的指定修订）
     *
     * @param articleId  文章标识
     * @param revisionId 修订标识
     * @return 文章详情
     */
    public Map<String, Object> articleDetail(String articleId, String revisionId) {
        ExploreArticle article = articleRepository.findByArticleId(articleId)
                .orElseThrow(() -> notFound("文章不存在"));
        if (article.getStatus() != ExploreContentStatus.PUBLISHED) {
            throw notFound("文章已下线");
        }
        ExploreArticleRevision revision = articleRepository.findRevision(revisionId)
                .orElseThrow(() -> notFound("文章修订不存在"));
        if (!articleId.equals(revision.getArticleId())
                || revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
            throw notFound("文章修订未发布");
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("articleId", article.getArticleId());
        view.put("revisionId", revision.getRevisionId());
        view.put("sourceType", article.getSourceType().name());
        view.put("title", revision.getTitle());
        view.put("summary", revision.getSummary());
        List<ExploreImage> images = articleRepository.findRevisionImages(revisionId);
        if (images.isEmpty() && revision.getCover() != null) {
            images = List.of(revision.getCover());
        }
        view.put("cover", imageView(images.isEmpty() ? null : images.get(0)));
        view.put("images", images.stream().map(this::imageView).toList());
        view.put("imageCount", images.size());
        view.put("imageLayout", ExploreImageLayout.of(images.size()));
        view.put("authorName", revision.getAuthorName());
        view.put("authorAvatarUrl", revision.getAuthorAvatarUrl());
        view.put("displayAt", iso(revision.getDisplayAt()));
        view.put("contentHash", revision.getContentHash());
        view.put("publishedAt", iso(revision.getPublishedAt()));
        if (article.getSourceType() == ExploreSourceType.OFFICIAL) {
            view.put("contentType", revision.getContentType());
            view.put("body", revision.getBody());
        } else {
            view.put("sourceContentId", revision.getSourceContentId());
            view.put("nativeRouteCode", revision.getNativeRouteCode());
        }
        return view;
    }

    /**
     * 专题详情（指定已发布修订 + 稳定分页文章）
     *
     * @param topicId    专题标识
     * @param revisionId 修订标识
     * @param cursor     不透明游标
     * @param pageSize   页大小
     * @return 专题详情
     */
    public Map<String, Object> topicDetail(String topicId, String revisionId, String cursor,
                                           Integer pageSize) {
        Instant now = Instant.now(clock);
        int size = normalizePageSize(pageSize);
        ExploreTopic topic = topicRepository.findByTopicId(topicId)
                .orElseThrow(() -> notFound("专题不存在"));
        if (topic.getStatus() != ExploreContentStatus.PUBLISHED) {
            throw notFound("专题已下线");
        }
        ExploreTopicRevision revision = topicRepository.findRevision(revisionId)
                .orElseThrow(() -> notFound("专题修订不存在"));
        if (!topicId.equals(revision.getTopicId())
                || revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
            throw notFound("专题修订未发布");
        }
        Integer lastSortNo = null;
        if (StringUtils.hasText(cursor)) {
            ExploreCursor decoded = cursorCodec.decode(cursor);
            if (!topicId.equals(decoded.getReleaseId()) || !revisionId.equals(decoded.getComponentId())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "游标与专题修订不匹配");
            }
            lastSortNo = decoded.getSortNo();
        }
        List<ExploreTopicItem> raw = topicRepository.listItemsAfter(revisionId, lastSortNo, size + 1);
        boolean hasMore = raw.size() > size;
        List<ExploreTopicItem> pageItems = hasMore ? raw.subList(0, size) : raw;
        List<ExploreTopicItem> visible = visibleTopicItems(pageItems);
        String nextCursor = hasMore
                ? cursorCodec.encode(topicId, revisionId, raw.get(size - 1).getSortNo(), "topic")
                : null;
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("topicId", topic.getTopicId());
        view.put("revisionId", revision.getRevisionId());
        view.put("name", revision.getName());
        view.put("summary", revision.getSummary());
        view.put("cover", imageView(revision.getCover()));
        view.put("target", revision.getTarget() == null ? Map.of() : revision.getTarget().toView());
        view.put("items", visible.stream()
                .map(item -> item.getDisplaySnapshot() == null
                        ? new LinkedHashMap<String, Object>() : new LinkedHashMap<>(item.getDisplaySnapshot()))
                .toList());
        view.put("hasMore", hasMore);
        if (nextCursor != null) {
            view.put("nextCursor", nextCursor);
        }
        return view;
    }

    private ExploreComponentView buildFeedComponent(ExploreComponentData data, int pageSize,
                                                    Instant now) {
        List<ExplorePageItem> raw = pageRepository.feedPage(data.getComponent().getComponentId(),
                null, null, pageSize + 1);
        boolean hasMore = raw.size() > pageSize;
        List<ExplorePageItem> pageItems = hasMore ? raw.subList(0, pageSize) : raw;
        List<ExplorePageItem> visible = visibleItems(pageItems, now);
        String nextCursor = hasMore
                ? cursorCodec.encode(data.getComponent().getPageId(),
                data.getComponent().getComponentId(), raw.get(pageSize - 1).getSortNo(),
                raw.get(pageSize - 1).getItemId())
                : null;
        return ExploreComponentView.builder()
                .type(data.getComponent().getComponentType().name())
                .componentId(data.getComponent().getComponentId())
                .sortNo(data.getComponent().getSortNo())
                .config(componentConfig(data.getComponent().getComponentType(),
                        data.getComponent().configView()))
                .items(new ArrayList<>(visible.stream().map(ExplorePageItem::displayView).toList()))
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .build();
    }

    private ExplorePageView pageView(ExploreChannel channel, ExplorePage page,
                                     List<ExploreComponentView> components, boolean preview) {
        return ExplorePageView.builder()
                .channelCode(channel.getChannelCode())
                .channelName(channel.getName())
                .releaseId(page.getPageId())
                .publishedAt(iso(page.getPublishedAt()))
                .preview(preview)
                .components(components)
                .build();
    }

    private ExploreChannel requireMobileChannel(String channelCode) {
        ExploreChannel channel = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "频道不存在: " + channelCode));
        if (channel.getStatus() != ExploreChannelStatus.ENABLED) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, "频道未启用");
        }
        return channel;
    }

    private ExplorePage requireCurrentPage(String channelCode, Instant now) {
        ExplorePage page = pageRepository.findCurrentPage(channelCode)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "频道当前无有效页面"));
        if (page.getStatus() != ExplorePageStatus.PUBLISHED
                || (page.getEffectiveAt() != null && page.getEffectiveAt().isAfter(now))
                || (page.getExpireAt() != null && !page.getExpireAt().isAfter(now))) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, "频道当前无有效页面");
        }
        return page;
    }

    private List<ExplorePageItem> visibleItems(List<ExplorePageItem> items, Instant now) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<String> articleIds = items.stream()
                .filter(item -> item.getItemType() == ExploreItemType.ARTICLE)
                .map(ExplorePageItem::getRefId).collect(Collectors.toSet());
        Set<String> topicIds = items.stream()
                .filter(item -> item.getItemType() == ExploreItemType.TOPIC)
                .map(ExplorePageItem::getRefId).collect(Collectors.toSet());
        Set<String> bannerIds = items.stream()
                .filter(item -> item.getItemType() == ExploreItemType.BANNER)
                .map(ExplorePageItem::getRefId).collect(Collectors.toSet());
        Map<String, ExploreContentStatus> statuses = new HashMap<>();
        for (ExploreArticle article : articleRepository.findByIds(articleIds)) {
            statuses.put("ARTICLE:" + article.getArticleId(), article.getStatus());
        }
        for (ExploreTopic topic : topicRepository.findByIds(topicIds)) {
            statuses.put("TOPIC:" + topic.getTopicId(), topic.getStatus());
        }
        for (ExploreBanner banner : bannerRepository.findByIds(bannerIds)) {
            statuses.put("BANNER:" + banner.getBannerId(), banner.getStatus());
        }
        return items.stream()
                .filter(item -> isInTimeRange(item, now))
                .filter(item -> statuses.get(item.getItemType().name() + ":" + item.getRefId())
                        == ExploreContentStatus.PUBLISHED)
                .collect(Collectors.toList());
    }

    private List<ExploreTopicItem> visibleTopicItems(List<ExploreTopicItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<String> articleIds = items.stream()
                .map(item -> String.valueOf(item.getDisplaySnapshot().get("articleId")))
                .collect(Collectors.toSet());
        Set<String> published = articleRepository.findByIds(articleIds).stream()
                .filter(article -> article.getStatus() == ExploreContentStatus.PUBLISHED)
                .map(ExploreArticle::getArticleId)
                .collect(Collectors.toSet());
        return items.stream()
                .filter(item -> published.contains(String.valueOf(item.getDisplaySnapshot().get("articleId"))))
                .collect(Collectors.toList());
    }

    private boolean isInTimeRange(ExplorePageItem item, Instant now) {
        if (item.getStartAt() != null && item.getStartAt().isAfter(now)) {
            return false;
        }
        return item.getEndAt() == null || item.getEndAt().isAfter(now);
    }

    private Map<String, Object> componentConfig(ExploreComponentType type, Map<String, Object> config) {
        Map<String, Object> view = new LinkedHashMap<>(config);
        if (type == ExploreComponentType.BANNER_CAROUSEL) {
            view.putIfAbsent("intervalSeconds", 5);
        } else if (type == ExploreComponentType.TOPIC_GRID) {
            view.putIfAbsent("style", "ADAPTIVE_GRID");
            if ("ADAPTIVE_GRID".equals(view.get("style"))) {
                view.putIfAbsent("columns", 4);
            }
        }
        return view;
    }

    private Map<String, Object> channelView(ExploreChannel channel) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("code", channel.getChannelCode());
        view.put("name", channel.getName());
        view.put("sortNo", channel.getSortNo());
        return view;
    }

    private Map<String, Object> imageView(ExploreImage image) {
        if (image == null || !StringUtils.hasText(image.getUrl())) {
            return null;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("url", image.getUrl());
        view.put("width", image.getWidth());
        view.put("height", image.getHeight());
        view.put("mime", image.getMime());
        view.put("size", image.getSize());
        return view;
    }

    private int normalizePageSize(Integer pageSize) {
        int defaultValue = properties.getFeedPageSizeDefault();
        int max = properties.getFeedPageSizeMax();
        if (pageSize == null) {
            return defaultValue;
        }
        if (pageSize <= 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "pageSize 必须为正数");
        }
        return Math.min(pageSize, max);
    }

    private String iso(Instant value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value);
    }

    private ExploreException notFound(String detail) {
        return new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND, detail);
    }
}
