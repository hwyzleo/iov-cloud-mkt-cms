package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExplorePageSaveCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExplorePagePublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePageData;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreAction;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreItemType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreGuard;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.ExploreIdGenerator;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.TraceContext;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreComponentView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreOperation;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 探索页频道页面应用服务（草稿保存、管理预览、发布历史与复制）。
 * <p>
 * MKT-CMS-DSN-CR-003 §5.1 / D-011 / D-015：每频道一个可编辑草稿，三类固定组件人工排序；
 * 保存携带 lockVersion，冲突不静默覆盖；预览读取草稿，不影响线上。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExplorePageAppService {

    private static final int MAX_COMPONENTS = 20;

    private static final int MAX_ITEMS_PER_COMPONENT = 200;

    private final ExplorePageRepository pageRepository;
    private final ExploreChannelRepository channelRepository;
    private final ExploreOperationRepository operationRepository;
    private final ExplorePageContentService contentService;
    private final Clock clock;

    /**
     * 读取频道草稿（不存在时返回空草稿视图，lockVersion=0）
     *
     * @param channelCode 频道编码
     * @return 草稿视图
     */
    public ExplorePageData getDraft(String channelCode) {
        requireChannel(channelCode);
        ExplorePage page = pageRepository.findDraft(channelCode).orElse(null);
        List<ExploreComponentData> components = page == null
                ? List.of() : contentService.load(page.getPageId());
        return new ExplorePageData(page, components);
    }

    /**
     * 整体保存频道草稿（携带 lockVersion，冲突不覆盖）
     *
     * @param cmd 保存命令
     * @return 保存后的草稿视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExplorePageData save(ExplorePageSaveCmd cmd) {
        String operator = ExploreGuard.requireOperator(cmd.getOperator());
        String channelCode = ExploreGuard.requireText(cmd.getChannelCode(), "频道编码", 32);
        requireChannel(channelCode);
        int expectedLockVersion = ExploreGuard.requireLockVersion(cmd.getLockVersion());
        Instant now = Instant.now(clock);

        ExplorePage draft = pageRepository.findDraftForUpdate(channelCode).orElse(null);
        if (draft == null) {
            if (expectedLockVersion != 0) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                        "频道尚无草稿，lockVersion 应为 0");
            }
            draft = ExplorePage.draft(ExploreIdGenerator.pageId(), channelCode, operator, now);
            pageRepository.insert(draft);
        } else if (draft.getLockVersion() != expectedLockVersion) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "页面草稿已被修改，请基于最新乐观锁版本重试");
        }

        List<ExploreComponentData> content = buildContent(draft.getPageId(), cmd.getComponents(), operator, now);
        pageRepository.deletePageContent(draft.getPageId());
        for (ExploreComponentData data : content) {
            pageRepository.insertComponent(data.getComponent());
            for (ExplorePageItem item : data.getItems()) {
                pageRepository.insertItem(item);
            }
        }
        ExplorePage saved = draft.saved(operator, now);
        if (pageRepository.updateDraft(saved, expectedLockVersion) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT,
                    "页面草稿已被并发修改，请重试");
        }
        audit(ExploreAction.SAVE_DRAFT, draft.getPageId(), operator, now, "channelCode=" + channelCode);
        log.info("探索页草稿保存成功: channelCode={}, pageId={}, lockVersion={}",
                channelCode, draft.getPageId(), saved.getLockVersion());
        return getDraft(channelCode);
    }

    /**
     * 管理侧手机预览（与手机组件契约同构，读取草稿，不影响线上）
     *
     * @param pageId 草稿页面标识
     * @return 预览视图
     */
    public ExplorePageView preview(String pageId) {
        ExplorePage page = pageRepository.findById(pageId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "页面草稿不存在: " + pageId));
        if (page.getPageVersion() != 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, "仅草稿页面支持预览");
        }
        ExploreChannel channel = requireChannel(page.getChannelCode());
        List<ExploreComponentView> components = new ArrayList<>();
        for (ExploreComponentData data : contentService.load(page.getPageId())) {
            components.add(ExploreComponentView.builder()
                    .type(data.getComponent().getComponentType().name())
                    .componentId(data.getComponent().getComponentId())
                    .sortNo(data.getComponent().getSortNo())
                    .config(contentService.componentConfig(data.getComponent()))
                    .items(data.getItems().stream().map(ExplorePageItem::displayView).toList())
                    .hasMore(false)
                    .build());
        }
        return ExplorePageView.builder()
                .channelCode(channel.getChannelCode())
                .channelName(channel.getName())
                .releaseId(page.getPageId())
                .preview(true)
                .components(components)
                .build();
    }

    /**
     * 读取频道当前生效页面（用于发布弹窗展示 expectedReleaseId）
     *
     * @param channelCode 频道编码
     * @return 当前生效页面内容；不存在时 page 为空
     */
    public ExplorePageData getCurrentPage(String channelCode) {
        requireChannel(channelCode);
        ExplorePage page = pageRepository.findCurrentPage(channelCode).orElse(null);
        List<ExploreComponentData> components = page == null
                ? List.of() : contentService.load(page.getPageId());
        return new ExplorePageData(page, components);
    }

    /**
     * 发布历史检索（物理分页由调用方 PageHelper 提供）
     *
     * @param criteria 检索条件
     * @return 发布版本列表
     */
    public List<ExplorePage> searchReleases(ExplorePageSearchCriteria criteria) {
        return pageRepository.searchReleases(criteria);
    }

    /**
     * 发布版本快照详情
     *
     * @param releaseId 发布版本标识
     * @return 页面内容
     */
    public ExplorePageData releaseDetail(String releaseId) {
        ExplorePage page = pageRepository.findById(releaseId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "发布版本不存在: " + releaseId));
        if (page.getPageVersion() <= 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, "指定标识不是发布版本");
        }
        return new ExplorePageData(page, contentService.load(page.getPageId()));
    }

    /**
     * 历史版本复制为新草稿（不修改历史，不影响当前指针）
     *
     * @param releaseId 发布版本标识
     * @param operator  操作人
     * @return 草稿视图
     */
    @Transactional(rollbackFor = Exception.class)
    public ExplorePageData copyToDraft(String releaseId, String operator) {
        String currentOperator = ExploreGuard.requireOperator(operator);
        ExplorePage release = pageRepository.findById(releaseId)
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "发布版本不存在: " + releaseId));
        if (release.getPageVersion() <= 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, "指定标识不是发布版本");
        }
        Instant now = Instant.now(clock);
        ExplorePage draft = pageRepository.findDraftForUpdate(release.getChannelCode()).orElse(null);
        if (draft == null) {
            draft = ExplorePage.draft(ExploreIdGenerator.pageId(), release.getChannelCode(),
                    currentOperator, now);
            pageRepository.insert(draft);
        }
        List<ExploreComponentData> copied = contentService.copyToDraft(release, draft.getPageId());
        pageRepository.deletePageContent(draft.getPageId());
        for (ExploreComponentData data : copied) {
            pageRepository.insertComponent(data.getComponent());
            for (ExplorePageItem item : data.getItems()) {
                pageRepository.insertItem(item);
            }
        }
        ExplorePage saved = draft.saved(currentOperator, now);
        if (pageRepository.updateDraft(saved, draft.getLockVersion()) == 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, "页面草稿已被并发修改，请重试");
        }
        audit(ExploreAction.COPY_TO_DRAFT, releaseId, currentOperator, now,
                "channelCode=" + release.getChannelCode());
        log.info("探索页历史版本复制为新草稿成功: releaseId={}, channelCode={}",
                releaseId, release.getChannelCode());
        return getDraft(release.getChannelCode());
    }

    /**
     * 校验发布命令时间并规范化（发布服务调用）
     *
     * @param cmd 发布命令
     */
    public void validatePublishTimes(ExplorePagePublishCmd cmd) {
        Instant now = Instant.now(clock);
        String publishType = ExploreGuard.requireText(cmd.getPublishType(), "发布类型", 16)
                .toUpperCase(java.util.Locale.ROOT);
        if ("SCHEDULED".equals(publishType)) {
            if (cmd.getEffectiveAt() == null) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "定时发布必须提供生效时间");
            }
            if (!cmd.getEffectiveAt().isAfter(now)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "定时发布时间必须晚于服务端当前时间");
            }
        } else if ("IMMEDIATE".equals(publishType)) {
            if (cmd.getEffectiveAt() != null && cmd.getEffectiveAt().isAfter(now)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "立即发布不接受未来生效时间，请使用定时发布");
            }
        } else {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "不支持的发布类型: " + cmd.getPublishType());
        }
        if (cmd.getExpireAt() != null) {
            Instant base = cmd.getEffectiveAt() == null ? now : cmd.getEffectiveAt();
            if (!cmd.getExpireAt().isAfter(base)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "到期时间必须晚于生效时间");
            }
        }
    }

    /**
     * 读取并校验频道
     *
     * @param channelCode 频道编码
     * @return 频道
     */
    public ExploreChannel requireChannel(String channelCode) {
        if (channelCode == null || channelCode.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, "频道编码不能为空");
        }
        return channelRepository.findByCode(channelCode.trim())
                .orElseThrow(() -> new ExploreException(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND,
                        "频道不存在: " + channelCode));
    }

    private List<ExploreComponentData> buildContent(String pageId, List<ExplorePageSaveCmd.Component> components,
                                                    String operator, Instant now) {
        if (components == null || components.isEmpty()) {
            return List.of();
        }
        if (components.size() > MAX_COMPONENTS) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "每页最多 " + MAX_COMPONENTS + " 个组件");
        }
        Set<Integer> componentSorts = new HashSet<>();
        List<ExploreComponentData> result = new ArrayList<>();
        for (ExplorePageSaveCmd.Component cmd : components) {
            ExploreComponentType type = ExploreComponentType.parse(cmd.getType());
            if (cmd.getSortNo() == null || cmd.getSortNo() < 0 || !componentSorts.add(cmd.getSortNo())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "组件排序不能为空、不得为负且不得重复");
            }
            String componentId = ExploreIdGenerator.componentId();
            ExplorePageComponent component = ExplorePageComponent.builder()
                    .componentId(componentId)
                    .pageId(pageId)
                    .componentType(type)
                    .sortNo(cmd.getSortNo())
                    .config(normalizeConfig(type, cmd.getConfig()))
                    .build();
            List<ExplorePageItem> items = buildItems(type, componentId, cmd.getItems(), operator, now);
            result.add(new ExploreComponentData(component, items));
        }
        return result;
    }

    private List<ExplorePageItem> buildItems(ExploreComponentType componentType, String componentId,
                                             List<ExplorePageSaveCmd.Item> items, String operator, Instant now) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (items.size() > MAX_ITEMS_PER_COMPONENT) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "单个组件条目不得超过 " + MAX_ITEMS_PER_COMPONENT);
        }
        Set<Integer> sorts = new HashSet<>();
        List<ExplorePageItem> result = new ArrayList<>();
        for (ExplorePageSaveCmd.Item cmd : items) {
            ExploreItemType itemType = ExploreItemType.parse(cmd.getItemType());
            if (!expectedItemType(componentType).equals(itemType)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        componentType.name() + " 组件只能引用 " + expectedItemType(componentType).name());
            }
            if (cmd.getSortNo() == null || cmd.getSortNo() < 0 || !sorts.add(cmd.getSortNo())) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "条目排序不能为空、不得为负且不得重复");
            }
            ExplorePageItem item = ExplorePageItem.builder()
                    .itemId(ExploreIdGenerator.itemId())
                    .componentId(componentId)
                    .itemType(itemType)
                    .refId(cmd.getRefId())
                    .refRevisionId(cmd.getRefRevisionId())
                    .sortNo(cmd.getSortNo())
                    .startAt(cmd.getStartAt())
                    .endAt(cmd.getEndAt())
                    .build();
            result.add(contentService.resolveDraftItem(item));
        }
        return result;
    }

    private int configInt(Object value, int defaultValue, String field) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    field + " 必须为整数");
        }
    }

    private Map<String, Object> normalizeConfig(ExploreComponentType type, Map<String, Object> raw) {
        Map<String, Object> config = raw == null ? new LinkedHashMap<>() : new LinkedHashMap<>(raw);
        Object title = config.get("title");
        if (title != null) {
            String normalized = ExploreGuard.optionalText(String.valueOf(title), "组件标题", 64);
            if (normalized == null) {
                config.remove("title");
            } else {
                config.put("title", normalized);
            }
        }
        if (type == ExploreComponentType.TOPIC_GRID) {
            String style = config.get("style") == null ? "ADAPTIVE_GRID"
                    : String.valueOf(config.get("style")).trim().toUpperCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("ADAPTIVE_GRID", "HORIZONTAL_SCROLL", "IMAGE_TEXT_LIST").contains(style)) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "不支持的专题展示样式: " + style);
            }
            config.put("style", style);
            if ("ADAPTIVE_GRID".equals(style)) {
                int columns = configInt(config.get("columns"), 4, "专题网格列数");
                if (columns < 2 || columns > 4) {
                    throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                            "专题网格列数仅支持 2/3/4");
                }
                config.put("columns", columns);
            } else {
                config.remove("columns");
            }
        }
        if (type == ExploreComponentType.BANNER_CAROUSEL) {
            int interval = configInt(config.get("intervalSeconds"), 5, "Banner 轮播间隔");
            if (interval < 3 || interval > 10) {
                throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                        "Banner 轮播间隔仅支持 3～10 秒");
            }
            config.put("intervalSeconds", interval);
        }
        return config;
    }

    private ExploreItemType expectedItemType(ExploreComponentType componentType) {
        return switch (componentType) {
            case BANNER_CAROUSEL -> ExploreItemType.BANNER;
            case TOPIC_GRID -> ExploreItemType.TOPIC;
            case ARTICLE_FEED -> ExploreItemType.ARTICLE;
        };
    }

    private void audit(ExploreAction action, String pageId, String operator, Instant now, String detail) {
        operationRepository.insert(ExploreOperation.success(ExploreIdGenerator.operationId(),
                action, "EXPLORE_PAGE", pageId, operator, now, null,
                TraceContext.currentTraceId(), detail));
    }
}
