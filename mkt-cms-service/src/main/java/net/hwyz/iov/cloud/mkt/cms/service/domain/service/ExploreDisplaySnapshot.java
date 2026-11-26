package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页卡片展示快照工厂。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.2 / D-016：display_snapshot 只保存经校验的卡片展示字段，
 * 不是任意业务 JSON；每个 itemType 使用固定 Schema 与版本号。
 *
 * @author hwyz_leo
 */
public final class ExploreDisplaySnapshot {

    /**
     * 快照 Schema 版本
     */
    public static final int SCHEMA_VERSION = 1;

    private ExploreDisplaySnapshot() {
    }

    /**
     * 文章卡片快照
     *
     * @param articleId   文章标识
     * @param revisionId  修订标识
     * @param sourceType  来源类型
     * @param title       标题
     * @param summary     摘要
     * @param cover       封面
     * @param authorName  作者显示名
     * @param avatarUrl   作者头像
     * @param displayAt   展示时间（ISO-8601）
     * @return 快照
     */
    public static Map<String, Object> article(String articleId, String revisionId, String sourceType,
                                              String title, String summary, List<ExploreImage> images,
                                              String authorName, String avatarUrl, String displayAt) {
        Map<String, Object> snapshot = base("ARTICLE");
        snapshot.put("articleId", articleId);
        snapshot.put("revisionId", revisionId);
        snapshot.put("sourceType", sourceType);
        snapshot.put("title", title);
        snapshot.put("summary", summary);
        List<ExploreImage> safeImages = images == null ? List.of() : images;
        putImage(snapshot, "cover", safeImages.isEmpty() ? null : safeImages.get(0));
        snapshot.put("images", safeImages.stream().map(ExploreDisplaySnapshot::imageView).toList());
        snapshot.put("imageCount", safeImages.size());
        snapshot.put("imageLayout", ExploreImageLayout.of(safeImages.size()));
        snapshot.put("authorName", authorName);
        snapshot.put("authorAvatarUrl", avatarUrl);
        snapshot.put("displayAt", displayAt);
        snapshot.put("target", articleTarget(articleId, revisionId));
        return snapshot;
    }

    /**
     * Banner 卡片快照
     *
     * @param bannerId   Banner 标识
     * @param revisionId 修订标识
     * @param title      标题
     * @param image      主图
     * @param target     跳转目标
     * @return 快照
     */
    public static Map<String, Object> banner(String bannerId, String revisionId, String title,
                                             ExploreImage image, ExploreTarget target) {
        Map<String, Object> snapshot = base("BANNER");
        snapshot.put("bannerId", bannerId);
        snapshot.put("revisionId", revisionId);
        snapshot.put("title", title);
        putImage(snapshot, "image", image);
        snapshot.put("target", target == null ? Map.of() : target.toView());
        return snapshot;
    }

    /**
     * 专题卡片快照（目标固定为专题自身详情）
     *
     * @param topicId    专题标识
     * @param revisionId 修订标识
     * @param name       名称
     * @param summary    摘要
     * @param cover      封面
     * @return 快照
     */
    public static Map<String, Object> topic(String topicId, String revisionId, String name,
                                            String summary, ExploreImage cover) {
        Map<String, Object> snapshot = base("TOPIC");
        snapshot.put("topicId", topicId);
        snapshot.put("revisionId", revisionId);
        snapshot.put("name", name);
        snapshot.put("summary", summary);
        putImage(snapshot, "cover", cover);
        snapshot.put("target", topicTarget(topicId, revisionId));
        return snapshot;
    }

    /**
     * 文章详情目标
     *
     * @param articleId  文章标识
     * @param revisionId 修订标识
     * @return 目标视图
     */
    public static Map<String, Object> articleTarget(String articleId, String revisionId) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("type", ExploreTargetType.ARTICLE.name());
        target.put("articleId", articleId);
        target.put("revisionId", revisionId);
        return target;
    }

    /**
     * 专题详情目标
     *
     * @param topicId    专题标识
     * @param revisionId 修订标识
     * @return 目标视图
     */
    public static Map<String, Object> topicTarget(String topicId, String revisionId) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("type", ExploreTargetType.TOPIC.name());
        target.put("topicId", topicId);
        target.put("revisionId", revisionId);
        return target;
    }

    private static Map<String, Object> base(String itemType) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", SCHEMA_VERSION);
        snapshot.put("itemType", itemType);
        return snapshot;
    }

    private static Map<String, Object> imageView(ExploreImage image) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("url", image.getUrl());
        view.put("width", image.getWidth());
        view.put("height", image.getHeight());
        view.put("mime", image.getMime());
        view.put("size", image.getSize());
        return view;
    }

    private static void putImage(Map<String, Object> snapshot, String field, ExploreImage image) {
        if (image == null || !StringUtils.hasText(image.getUrl())) {
            snapshot.put(field, null);
            return;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("url", image.getUrl());
        view.put("width", image.getWidth());
        view.put("height", image.getHeight());
        view.put("mime", image.getMime());
        view.put("size", image.getSize());
        snapshot.put(field, view);
    }
}
