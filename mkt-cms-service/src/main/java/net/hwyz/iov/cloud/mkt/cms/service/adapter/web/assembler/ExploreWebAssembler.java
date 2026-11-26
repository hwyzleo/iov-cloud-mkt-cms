package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler;

import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreArticleRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreBannerRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreChannelUpdateRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreContentPublishRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreImageRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExplorePagePublishRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExplorePageSaveRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreTargetRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreTopicRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreArticleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreBannerCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreContentPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExplorePagePublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExplorePageSaveCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreTopicCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreArticleView;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreBannerView;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePageData;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePublishResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreTopicView;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBannerRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页 Web 层显式转换（请求 VO → 应用命令；应用/领域视图 → 响应 Map）。
 * <p>
 * 响应字段保持契约稳定：列表不返回正文；详情返回清洗后的实际正文；
 * 时间统一带 offset 的 ISO-8601。
 *
 * @author hwyz_leo
 */
public final class ExploreWebAssembler {

    private ExploreWebAssembler() {
    }

    public static ExploreChannelCmd toCmd(String channelCode, ExploreChannelUpdateRequest request,
                                          String operator) {
        return ExploreChannelCmd.builder()
                .channelCode(channelCode)
                .name(request.getName())
                .sortNo(request.getSortNo())
                .status(request.getStatus())
                .lockVersion(request.getLockVersion())
                .operator(operator)
                .build();
    }

    public static ExploreArticleCmd toCmd(String articleId, ExploreArticleRequest request, String operator) {
        return ExploreArticleCmd.builder()
                .articleId(articleId)
                .sourceType(request.getSourceType())
                .title(request.getTitle())
                .summary(request.getSummary())
                .cover(toImage(request.getCover()))
                .authorName(request.getAuthorName())
                .authorAvatarUrl(request.getAuthorAvatarUrl())
                .displayAt(request.getDisplayAt())
                .body(request.getBody())
                .sourceContentId(request.getSourceContentId())
                .nativeRouteCode(request.getNativeRouteCode())
                .lockVersion(request.getLockVersion())
                .operator(operator)
                .build();
    }

    public static ExploreTopicCmd toCmd(String topicId, ExploreTopicRequest request, String operator) {
        return ExploreTopicCmd.builder()
                .topicId(topicId)
                .name(request.getName())
                .summary(request.getSummary())
                .cover(toImage(request.getCover()))
                .target(toTarget(request.getTarget()))
                .articleRevisionIds(request.getArticleRevisionIds())
                .lockVersion(request.getLockVersion())
                .operator(operator)
                .build();
    }

    public static ExploreBannerCmd toCmd(String bannerId, ExploreBannerRequest request, String operator) {
        return ExploreBannerCmd.builder()
                .bannerId(bannerId)
                .title(request.getTitle())
                .image(toImage(request.getImage()))
                .target(toTarget(request.getTarget()))
                .defaultSortNo(request.getDefaultSortNo())
                .lockVersion(request.getLockVersion())
                .operator(operator)
                .build();
    }

    public static ExplorePageSaveCmd toCmd(String channelCode, ExplorePageSaveRequest request, String operator) {
        List<ExplorePageSaveCmd.Component> components = new ArrayList<>();
        if (request.getComponents() != null) {
            for (ExplorePageSaveRequest.Component component : request.getComponents()) {
                List<ExplorePageSaveCmd.Item> items = new ArrayList<>();
                if (component.getItems() != null) {
                    for (ExplorePageSaveRequest.Item item : component.getItems()) {
                        items.add(ExplorePageSaveCmd.Item.builder()
                                .itemType(item.getItemType())
                                .refId(item.getRefId())
                                .refRevisionId(item.getRefRevisionId())
                                .sortNo(item.getSortNo())
                                .startAt(item.getStartAt())
                                .endAt(item.getEndAt())
                                .target(toTarget(item.getTarget()))
                                .build());
                    }
                }
                components.add(ExplorePageSaveCmd.Component.builder()
                        .type(component.getType())
                        .sortNo(component.getSortNo())
                        .config(component.getConfig())
                        .items(items)
                        .build());
            }
        }
        return ExplorePageSaveCmd.builder()
                .channelCode(channelCode)
                .lockVersion(request.getLockVersion())
                .components(components)
                .operator(operator)
                .build();
    }

    public static ExplorePagePublishCmd toCmd(String channelCode, ExplorePagePublishRequest request,
                                               String operator) {
        return ExplorePagePublishCmd.builder()
                .channelCode(channelCode)
                .requestId(request.getRequestId())
                .expectedReleaseId(request.getExpectedReleaseId())
                .publishType(request.getPublishType())
                .effectiveAt(request.getEffectiveAt())
                .expireAt(request.getExpireAt())
                .operator(operator)
                .build();
    }

    public static ExploreContentPublishCmd toContentPublishCmd(String objectId,
                                                               ExploreContentPublishRequest request,
                                                               String operator) {
        return ExploreContentPublishCmd.builder()
                .objectId(objectId)
                .revisionId(request == null ? null : request.getRevisionId())
                .operator(operator)
                .build();
    }

    public static ExploreImage toImage(ExploreImageRequest request) {
        if (request == null) {
            return null;
        }
        return ExploreImage.builder()
                .url(request.getUrl())
                .width(request.getWidth())
                .height(request.getHeight())
                .mime(request.getMime())
                .size(request.getSize())
                .build();
    }

    public static ExploreTarget toTarget(ExploreTargetRequest request) {
        if (request == null) {
            return null;
        }
        ExploreTargetType type = ExploreTargetType.parse(request.getType());
        String refId = type == ExploreTargetType.ARTICLE ? request.getArticleId()
                : type == ExploreTargetType.TOPIC ? request.getTopicId() : null;
        return ExploreTarget.builder()
                .type(type)
                .refId(refId)
                .revisionId(request.getRevisionId())
                .url(request.getUrl())
                .routeCode(request.getRouteCode())
                .params(request.getParams())
                .build();
    }

    public static Map<String, Object> toChannelView(ExploreChannel channel) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("channelCode", channel.getChannelCode());
        view.put("name", channel.getName());
        view.put("sortNo", channel.getSortNo());
        view.put("status", channel.getStatus() == null ? null : channel.getStatus().name());
        view.put("lockVersion", channel.getLockVersion());
        view.put("updatedAt", iso(channel.getModifyTime()));
        return view;
    }

    public static Map<String, Object> toArticleListView(ExploreArticleView data) {
        Map<String, Object> view = articleBase(data);
        view.remove("body");
        return view;
    }

    public static Map<String, Object> toArticleView(ExploreArticleView data) {
        return articleBase(data);
    }

    public static Map<String, Object> toTopicListView(ExploreTopicView data) {
        return topicBase(data, false);
    }

    public static Map<String, Object> toTopicView(ExploreTopicView data) {
        return topicBase(data, true);
    }

    public static Map<String, Object> toBannerListView(ExploreBannerView data) {
        return bannerBase(data);
    }

    public static Map<String, Object> toBannerView(ExploreBannerView data) {
        return bannerBase(data);
    }

    public static Map<String, Object> toDraftView(ExplorePageData data) {
        ExplorePage page = data.getPage();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("channelCode", page == null ? null : page.getChannelCode());
        view.put("pageId", page == null ? null : page.getPageId());
        view.put("pageVersion", page == null ? 0 : page.getPageVersion());
        view.put("status", page == null ? "DRAFT" : page.getStatus().name());
        view.put("lockVersion", page == null ? 0 : page.getLockVersion());
        view.put("exists", page != null);
        view.put("components", components(data, true));
        return view;
    }

    public static Map<String, Object> toReleaseView(ExplorePage page) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("releaseId", page.getPageId());
        view.put("channelCode", page.getChannelCode());
        view.put("pageVersion", page.getPageVersion());
        view.put("status", page.getStatus().name());
        view.put("effectiveAt", iso(page.getEffectiveAt()));
        view.put("expireAt", iso(page.getExpireAt()));
        view.put("publishedAt", iso(page.getPublishedAt()));
        view.put("publishedBy", page.getPublishedBy());
        view.put("requestId", page.getRequestId());
        view.put("expectedReleaseId", page.getExpectedReleaseId());
        return view;
    }

    public static Map<String, Object> toCurrentReleaseView(ExplorePage page) {
        if (page == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("releaseId", null);
            return empty;
        }
        return toReleaseView(page);
    }

    public static Map<String, Object> toReleaseDetailView(ExplorePageData data) {
        Map<String, Object> view = toReleaseView(data.getPage());
        view.put("components", components(data, false));
        return view;
    }

    public static Map<String, Object> toPublishView(ExplorePublishResult result) {
        return toReleaseView(result.getPage());
    }

    private static Map<String, Object> articleBase(ExploreArticleView data) {
        ExploreArticle article = data.getArticle();
        ExploreArticleRevision revision = data.getRevision();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("articleId", article.getArticleId());
        view.put("sourceType", article.getSourceType() == null ? null : article.getSourceType().name());
        view.put("status", article.getStatus() == null ? null : article.getStatus().name());
        view.put("currentRevisionId", article.getCurrentRevisionId());
        view.put("lockVersion", article.getLockVersion());
        view.put("revisionId", revision == null ? null : revision.getRevisionId());
        view.put("revisionNo", revision == null ? null : revision.getRevisionNo());
        view.put("revisionStatus", revision == null || revision.getStatus() == null
                ? null : revision.getStatus().name());
        view.put("title", revision == null ? null : revision.getTitle());
        view.put("summary", revision == null ? null : revision.getSummary());
        view.put("cover", image(revision == null ? null : revision.getCover()));
        view.put("authorName", revision == null ? null : revision.getAuthorName());
        view.put("authorAvatarUrl", revision == null ? null : revision.getAuthorAvatarUrl());
        view.put("displayAt", revision == null ? null : iso(revision.getDisplayAt()));
        view.put("contentType", revision == null ? null : revision.getContentType());
        view.put("body", revision == null ? null : revision.getBody());
        view.put("contentHash", revision == null ? null : revision.getContentHash());
        view.put("sanitizerVersion", revision == null ? null : revision.getSanitizerVersion());
        view.put("sourceContentId", revision == null ? null : revision.getSourceContentId());
        view.put("nativeRouteCode", revision == null ? null : revision.getNativeRouteCode());
        view.put("publishedAt", revision == null ? null : iso(revision.getPublishedAt()));
        view.put("updatedAt", iso(article.getModifyTime()));
        return view;
    }

    private static Map<String, Object> topicBase(ExploreTopicView data, boolean includeItems) {
        ExploreTopic topic = data.getTopic();
        ExploreTopicRevision revision = data.getRevision();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("topicId", topic.getTopicId());
        view.put("status", topic.getStatus() == null ? null : topic.getStatus().name());
        view.put("currentRevisionId", topic.getCurrentRevisionId());
        view.put("lockVersion", topic.getLockVersion());
        view.put("revisionId", revision == null ? null : revision.getRevisionId());
        view.put("revisionNo", revision == null ? null : revision.getRevisionNo());
        view.put("revisionStatus", revision == null || revision.getStatus() == null
                ? null : revision.getStatus().name());
        view.put("name", revision == null ? null : revision.getName());
        view.put("summary", revision == null ? null : revision.getSummary());
        view.put("cover", image(revision == null ? null : revision.getCover()));
        view.put("target", revision == null || revision.getTarget() == null
                ? null : revision.getTarget().toView());
        view.put("publishedAt", revision == null ? null : iso(revision.getPublishedAt()));
        view.put("updatedAt", iso(topic.getModifyTime()));
        if (includeItems) {
            List<Map<String, Object>> items = new ArrayList<>();
            int index = 0;
            for (ExploreTopicItem item : data.getItems()) {
                index++;
                Map<String, Object> card = item.getDisplaySnapshot() == null
                        ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getDisplaySnapshot());
                card.put("articleRevisionId", item.getArticleRevisionId());
                card.put("sortNo", item.getSortNo());
                card.put("rowNo", index);
                items.add(card);
            }
            view.put("items", items);
        }
        return view;
    }

    private static Map<String, Object> bannerBase(ExploreBannerView data) {
        ExploreBanner banner = data.getBanner();
        ExploreBannerRevision revision = data.getRevision();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("bannerId", banner.getBannerId());
        view.put("status", banner.getStatus() == null ? null : banner.getStatus().name());
        view.put("currentRevisionId", banner.getCurrentRevisionId());
        view.put("lockVersion", banner.getLockVersion());
        view.put("revisionId", revision == null ? null : revision.getRevisionId());
        view.put("revisionNo", revision == null ? null : revision.getRevisionNo());
        view.put("revisionStatus", revision == null || revision.getStatus() == null
                ? null : revision.getStatus().name());
        view.put("title", revision == null ? null : revision.getTitle());
        view.put("image", image(revision == null ? null : revision.getImage()));
        view.put("target", revision == null || revision.getTarget() == null
                ? null : revision.getTarget().toView());
        view.put("defaultSortNo", revision == null ? null : revision.getDefaultSortNo());
        view.put("publishedAt", revision == null ? null : iso(revision.getPublishedAt()));
        view.put("updatedAt", iso(banner.getModifyTime()));
        return view;
    }

    private static List<Map<String, Object>> components(ExplorePageData data, boolean draft) {
        List<Map<String, Object>> components = new ArrayList<>();
        for (ExploreComponentData component : data.getComponents()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("componentId", component.getComponent().getComponentId());
            view.put("type", component.getComponent().getComponentType().name());
            view.put("sortNo", component.getComponent().getSortNo());
            if (component.getComponent().getConfig() != null
                    && !component.getComponent().getConfig().isEmpty()) {
                view.put("config", component.getComponent().getConfig());
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (ExplorePageItem item : component.getItems()) {
                Map<String, Object> itemView = new LinkedHashMap<>();
                itemView.put("itemId", item.getItemId());
                itemView.put("itemType", item.getItemType().name());
                itemView.put("refId", item.getRefId());
                itemView.put("refRevisionId", item.getRefRevisionId());
                itemView.put("sortNo", item.getSortNo());
                itemView.put("startAt", iso(item.getStartAt()));
                itemView.put("endAt", iso(item.getEndAt()));
                itemView.put("target", item.getTarget() == null ? null : item.getTarget().toView());
                itemView.put("display", item.getDisplaySnapshot());
                items.add(itemView);
            }
            view.put("items", items);
            components.add(view);
        }
        return components;
    }

    private static Map<String, Object> image(ExploreImage image) {
        if (image == null) {
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

    private static String iso(Instant value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value);
    }
}
