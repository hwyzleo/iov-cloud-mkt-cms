package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreItemType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreGuard;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBannerRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreBannerRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreDisplaySnapshot;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页页面内容解析与物化服务。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.2 / AC-036 / AC-043：发布时固定引用修订并生成展示快照；
 * 引用不存在、未发布或根实体已下线时指出具体条目并拒绝；发布版本内容永久不可变。
 *
 * @author hwyz_leo
 */
@Service
@RequiredArgsConstructor
public class ExplorePageContentService {

    private final ExplorePageRepository pageRepository;
    private final ExploreArticleRepository articleRepository;
    private final ExploreTopicRepository topicRepository;
    private final ExploreBannerRepository bannerRepository;
    private final ExploreTargetAppService targetAppService;

    /**
     * 读取页面组件与条目（未过滤，管理端使用）
     *
     * @param pageId 页面标识
     * @return 组件与条目
     */
    public List<ExploreComponentData> load(String pageId) {
        List<ExploreComponentData> data = new ArrayList<>();
        for (ExplorePageComponent component : pageRepository.listComponents(pageId)) {
            data.add(new ExploreComponentData(component, pageRepository.listItems(component.getComponentId())));
        }
        return data;
    }

    /**
     * 校验草稿并物化为目标页面内容（重新解析引用与展示快照）
     *
     * @param sourcePage   源页面（草稿）
     * @param targetPageId 目标版本标识
     * @return 物化后的组件与条目
     */
    public List<ExploreComponentData> materializePublish(ExplorePage sourcePage, String targetPageId) {
        List<ExploreComponentData> result = new ArrayList<>();
        for (ExploreComponentData data : load(sourcePage.getPageId())) {
            String componentId = ExploreIdGenerator.componentId();
            ExplorePageComponent component = data.getComponent().toBuilder()
                    .id(null)
                    .componentId(componentId)
                    .pageId(targetPageId)
                    .build();
            List<ExplorePageItem> items = new ArrayList<>();
            for (ExplorePageItem item : data.getItems()) {
                Resolved resolved = resolvePublished(item);
                items.add(item.toBuilder()
                        .id(null)
                        .itemId(ExploreIdGenerator.itemId())
                        .componentId(componentId)
                        .target(resolved.target)
                        .displaySnapshot(resolved.snapshot)
                        .build());
            }
            result.add(new ExploreComponentData(component, items));
        }
        return result;
    }

    /**
     * 复制发布版本内容为新草稿内容（保留展示快照，生成新标识）
     *
     * @param release    发布版本
     * @param draftPageId 目标草稿标识
     * @return 复制后的组件与条目
     */
    public List<ExploreComponentData> copyToDraft(ExplorePage release, String draftPageId) {
        List<ExploreComponentData> result = new ArrayList<>();
        for (ExploreComponentData data : load(release.getPageId())) {
            String componentId = ExploreIdGenerator.componentId();
            ExplorePageComponent component = data.getComponent().toBuilder()
                    .id(null)
                    .componentId(componentId)
                    .pageId(draftPageId)
                    .build();
            List<ExplorePageItem> items = new ArrayList<>();
            for (ExplorePageItem item : data.getItems()) {
                items.add(item.toBuilder()
                        .id(null)
                        .itemId(ExploreIdGenerator.itemId())
                        .componentId(componentId)
                        .build());
            }
            result.add(new ExploreComponentData(component, items));
        }
        return result;
    }

    /**
     * 校验并解析单个页面条目（保存草稿时使用）
     *
     * @param item 页面条目
     * @return 补全目标与展示快照后的条目
     */
    public ExplorePageItem resolveDraftItem(ExplorePageItem item) {
        ExploreGuard.validateTimeRange(item.getStartAt(), item.getEndAt());
        ExploreItemType itemType = item.getItemType();
        if (itemType == null) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "页面条目类型不能为空");
        }
        ExploreGuard.requireText(item.getRefId(), "页面条目引用标识", 64);
        ExploreGuard.requireText(item.getRefRevisionId(), "页面条目引用修订标识", 64);
        Resolved resolved = resolve(itemType, item.getRefId(), item.getRefRevisionId(), true);
        return item.toBuilder()
                .target(resolved.target)
                .displaySnapshot(resolved.snapshot)
                .build();
    }

    /**
     * 按引用类型解析展示快照与跳转目标
     *
     * @param itemType       条目类型
     * @param refId          引用业务标识
     * @param refRevisionId  引用修订标识
     * @param requireVisible 是否要求根实体未下线
     * @return 解析结果
     */
    public Resolved resolve(ExploreItemType itemType, String refId, String refRevisionId,
                            boolean requireVisible) {
        switch (itemType) {
            case ARTICLE -> {
                ExploreArticleRevision revision = articleRepository.findRevision(refRevisionId)
                        .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                                "页面条目文章修订不存在: " + refRevisionId));
                if (!refId.equals(revision.getArticleId())
                        || revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
                    throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                            "页面条目必须引用已发布文章修订: " + refRevisionId);
                }
                ExploreArticle article = articleRepository.findByArticleId(refId)
                        .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                                "页面条目文章不存在: " + refId));
                requireVisible(requireVisible, article.getStatus(), refId);
                Map<String, Object> snapshot = ExploreDisplaySnapshot.article(
                        article.getArticleId(), revision.getRevisionId(), article.getSourceType().name(),
                        revision.getTitle(), revision.getSummary(), revision.getCover(),
                        revision.getAuthorName(), revision.getAuthorAvatarUrl(),
                        revision.getDisplayAt() == null ? null
                                : DateTimeFormatter.ISO_INSTANT.format(revision.getDisplayAt()));
                return new Resolved(ExploreTarget.builder()
                        .type(ExploreTargetType.ARTICLE)
                        .refId(article.getArticleId())
                        .revisionId(revision.getRevisionId())
                        .build(), snapshot);
            }
            case TOPIC -> {
                ExploreTopicRevision revision = topicRepository.findRevision(refRevisionId)
                        .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                                "页面条目专题修订不存在: " + refRevisionId));
                if (!refId.equals(revision.getTopicId())
                        || revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
                    throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                            "页面条目必须引用已发布专题修订: " + refRevisionId);
                }
                ExploreTopic topic = topicRepository.findByTopicId(refId)
                        .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                                "页面条目专题不存在: " + refId));
                requireVisible(requireVisible, topic.getStatus(), refId);
                Map<String, Object> snapshot = ExploreDisplaySnapshot.topic(
                        topic.getTopicId(), revision.getRevisionId(), revision.getName(),
                        revision.getSummary(), revision.getCover());
                return new Resolved(ExploreTarget.builder()
                        .type(ExploreTargetType.TOPIC)
                        .refId(topic.getTopicId())
                        .revisionId(revision.getRevisionId())
                        .build(), snapshot);
            }
            case BANNER -> {
                ExploreBannerRevision revision = bannerRepository.findRevision(refRevisionId)
                        .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                                "页面条目 Banner 修订不存在: " + refRevisionId));
                if (!refId.equals(revision.getBannerId())
                        || revision.getStatus() != ExploreRevisionStatus.PUBLISHED) {
                    throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                            "页面条目必须引用已发布 Banner 修订: " + refRevisionId);
                }
                ExploreBanner banner = bannerRepository.findByBannerId(refId)
                        .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND,
                                "页面条目 Banner 不存在: " + refId));
                requireVisible(requireVisible, banner.getStatus(), refId);
                ExploreTarget target = revision.getTarget();
                if (target == null || target.getType() == null) {
                    throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                            "Banner 修订缺少跳转目标: " + refRevisionId);
                }
                targetAppService.validate(target);
                Map<String, Object> snapshot = ExploreDisplaySnapshot.banner(
                        banner.getBannerId(), revision.getRevisionId(), revision.getTitle(),
                        revision.getImage(), target);
                return new Resolved(target, snapshot);
            }
            default -> throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "不支持的页面条目类型");
        }
    }

    /**
     * 构造组件配置视图
     *
     * @param component 组件
     * @return 有序配置
     */
    public Map<String, Object> componentConfig(ExplorePageComponent component) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (component.getConfig() != null) {
            config.putAll(component.getConfig());
        }
        return config;
    }

    private Resolved resolvePublished(ExplorePageItem item) {
        return resolve(item.getItemType(), item.getRefId(), item.getRefRevisionId(), true);
    }

    private void requireVisible(boolean requireVisible, ExploreContentStatus status, String refId) {
        if (requireVisible && status != ExploreContentStatus.PUBLISHED) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "页面条目引用的内容已下线: " + refId);
        }
    }

    /**
     * 解析结果（展示快照 + 跳转目标）
     */
    public static final class Resolved {

        private final ExploreTarget target;

        private final Map<String, Object> snapshot;

        public Resolved(ExploreTarget target, Map<String, Object> snapshot) {
            this.target = target;
            this.snapshot = snapshot;
        }

        public ExploreTarget target() {
            return target;
        }

        public Map<String, Object> snapshot() {
            return snapshot;
        }
    }
}
