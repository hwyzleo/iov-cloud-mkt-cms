package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreBannerRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreChannelRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreCursorCodec;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config.ExploreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 探索页手机端文章流组件定位测试。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("手机探索页 - 多文章流翻页")
class ExploreMobileQueryAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    @Mock
    private ExploreChannelRepository channelRepository;
    @Mock
    private ExplorePageRepository pageRepository;
    @Mock
    private ExploreArticleRepository articleRepository;
    @Mock
    private ExploreTopicRepository topicRepository;
    @Mock
    private ExploreBannerRepository bannerRepository;
    @Mock
    private ExplorePageContentService contentService;
    @Mock
    private ExploreCursorCodec cursorCodec;

    private ExploreMobileQueryAppService mobileQueryAppService;

    @BeforeEach
    void setUp() {
        mobileQueryAppService = new ExploreMobileQueryAppService(channelRepository, pageRepository,
                articleRepository, topicRepository, bannerRepository, contentService, cursorCodec,
                new ExploreProperties(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("按 componentId 选择目标文章流，不回退到第一个文章流")
    void 按组件选择文章流() {
        stubCurrentPage();
        when(contentService.load("er_01")).thenReturn(List.of(
                component("ecp_01", ExploreComponentType.ARTICLE_FEED),
                component("ecp_02", ExploreComponentType.ARTICLE_FEED)));
        when(pageRepository.feedPage("ecp_02", null, null, 11)).thenReturn(List.of());

        Map<String, Object> result = mobileQueryAppService.articleFeed(
                "RECOMMEND", "ecp_02", "er_01", null, null);

        assertEquals("ecp_02", result.get("componentId"));
        assertFalse((Boolean) result.get("hasMore"));
        verify(pageRepository).feedPage("ecp_02", null, null, 11);
        verify(pageRepository, never()).feedPage("ecp_01", null, null, 11);
    }

    @Test
    @DisplayName("componentId 不是当前发布版本的文章流时返回参数错误")
    void 拒绝非文章流组件() {
        stubCurrentPage();
        when(contentService.load("er_01")).thenReturn(List.of(
                component("ecp_topic", ExploreComponentType.TOPIC_GRID),
                component("ecp_feed", ExploreComponentType.ARTICLE_FEED)));

        ExploreException exception = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.articleFeed(
                        "RECOMMEND", "ecp_topic", "er_01", null, null));

        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, exception.getErrorCode());
        verify(pageRepository, never()).feedPage(anyString(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("专题详情返回图集、数量与布局，封面取首图")
    void 专题详情图集契约() {
        when(topicRepository.findByTopicId("tp_01")).thenReturn(Optional.of(ExploreTopic.builder()
                .topicId("tp_01")
                .status(ExploreContentStatus.PUBLISHED)
                .currentRevisionId("tr_01")
                .lockVersion(1)
                .build()));
        when(topicRepository.findRevision("tr_01")).thenReturn(Optional.of(ExploreTopicRevision.builder()
                .revisionId("tr_01")
                .topicId("tp_01")
                .revisionNo(1)
                .status(ExploreRevisionStatus.PUBLISHED)
                .name("专题")
                .summary("摘要")
                .build()));
        when(topicRepository.findRevisionImages("tr_01"))
                .thenReturn(List.of(image("a.png"), image("b.png"), image("c.png")));
        when(topicRepository.listItemsAfter("tr_01", null, 11)).thenReturn(List.of());

        Map<String, Object> detail = mobileQueryAppService.topicDetail("tp_01", "tr_01", null, 10);

        assertEquals(3, detail.get("imageCount"));
        assertEquals("ONE_LARGE_TWO_SMALL", detail.get("imageLayout"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> images = (List<Map<String, Object>>) detail.get("images");
        assertEquals(3, images.size());
        assertEquals("https://assets.example/a.png", images.get(0).get("url"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cover = (Map<String, Object>) detail.get("cover");
        assertEquals("https://assets.example/a.png", cover.get("url"));
        assertFalse((Boolean) detail.get("hasMore"));
    }

    @Test
    @DisplayName("专题详情非空文章条目携带 itemId 与 sortNo，避免手机端全量丢弃")
    void 专题详情文章条目契约() {
        when(topicRepository.findByTopicId("tp_01")).thenReturn(Optional.of(ExploreTopic.builder()
                .topicId("tp_01")
                .status(ExploreContentStatus.PUBLISHED)
                .currentRevisionId("tr_01")
                .lockVersion(1)
                .build()));
        when(topicRepository.findRevision("tr_01")).thenReturn(Optional.of(ExploreTopicRevision.builder()
                .revisionId("tr_01")
                .topicId("tp_01")
                .revisionNo(1)
                .status(ExploreRevisionStatus.PUBLISHED)
                .name("专题")
                .summary("摘要")
                .build()));
        when(topicRepository.findRevisionImages("tr_01")).thenReturn(List.of(image("a.png")));
        when(topicRepository.listItemsAfter("tr_01", null, 11)).thenReturn(List.of(
                topicItem("arv_01", "ar_01", 10, "文章一"),
                topicItem("arv_02", "ar_02", 20, "文章二")));
        when(articleRepository.findByIds(any())).thenReturn(List.of(
                ExploreArticle.builder().articleId("ar_01").status(ExploreContentStatus.PUBLISHED).build(),
                ExploreArticle.builder().articleId("ar_02").status(ExploreContentStatus.PUBLISHED).build()));

        Map<String, Object> detail = mobileQueryAppService.topicDetail("tp_01", "tr_01", null, 10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) detail.get("items");
        assertEquals(2, items.size());
        assertEquals("arv_01", items.get(0).get("itemId"));
        assertEquals("ARTICLE", items.get(0).get("itemType"));
        assertEquals("ar_01", items.get(0).get("articleId"));
        assertEquals(10, items.get(0).get("sortNo"));
        assertEquals("文章一", items.get(0).get("title"));
        assertEquals("arv_02", items.get(1).get("itemId"));
        assertEquals(20, items.get(1).get("sortNo"));
        assertFalse((Boolean) detail.get("hasMore"));
    }

    @Test
    @DisplayName("专题详情 images/imageCount/imageLayout 只按 showInDetail=true 子集计算")
    void 专题详情仅返回详情可见子集() {
        stubPublishedTopicRevision();
        when(topicRepository.findRevisionImages("tr_01")).thenReturn(List.of(
                image("a.png"),
                image("b.png").toBuilder().showInDetail(false).build(),
                image("c.png").toBuilder().showInDetail(true).build()));
        when(topicRepository.listItemsAfter("tr_01", null, 11)).thenReturn(List.of());

        Map<String, Object> detail = mobileQueryAppService.topicDetail("tp_01", "tr_01", null, 10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> images = (List<Map<String, Object>>) detail.get("images");
        assertEquals(2, images.size());
        assertEquals("https://assets.example/a.png", images.get(0).get("url"));
        assertEquals("https://assets.example/c.png", images.get(1).get("url"));
        assertEquals(2, detail.get("imageCount"));
        assertEquals("TWO_COLUMNS", detail.get("imageLayout"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cover = (Map<String, Object>) detail.get("cover");
        assertEquals("https://assets.example/a.png", cover.get("url"));
    }

    @Test
    @DisplayName("全部图片 showInDetail=false 时详情显式为空，不回退封面")
    void 详情全隐藏时为空集合() {
        stubPublishedTopicRevision();
        when(topicRepository.findRevisionImages("tr_01")).thenReturn(List.of(
                image("a.png").toBuilder().showInDetail(false).build(),
                image("b.png").toBuilder().showInDetail(false).build()));
        when(topicRepository.listItemsAfter("tr_01", null, 11)).thenReturn(List.of());

        Map<String, Object> detail = mobileQueryAppService.topicDetail("tp_01", "tr_01", null, 10);

        assertEquals(0, detail.get("imageCount"));
        assertEquals("SINGLE", detail.get("imageLayout"));
        assertEquals(List.of(), detail.get("images"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cover = (Map<String, Object>) detail.get("cover");
        assertEquals("https://assets.example/a.png", cover.get("url"));
    }

    private void stubPublishedTopicRevision() {
        when(topicRepository.findByTopicId("tp_01")).thenReturn(Optional.of(ExploreTopic.builder()
                .topicId("tp_01")
                .status(ExploreContentStatus.PUBLISHED)
                .currentRevisionId("tr_01")
                .lockVersion(1)
                .build()));
        when(topicRepository.findRevision("tr_01")).thenReturn(Optional.of(ExploreTopicRevision.builder()
                .revisionId("tr_01")
                .topicId("tp_01")
                .revisionNo(1)
                .status(ExploreRevisionStatus.PUBLISHED)
                .name("专题")
                .summary("摘要")
                .build()));
    }

    private ExploreTopicItem topicItem(String articleRevisionId, String articleId, int sortNo,
                                       String title) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", 1);
        snapshot.put("itemType", "ARTICLE");
        snapshot.put("articleId", articleId);
        snapshot.put("title", title);
        return ExploreTopicItem.builder()
                .topicRevisionId("tr_01")
                .articleRevisionId(articleRevisionId)
                .sortNo(sortNo)
                .displaySnapshot(snapshot)
                .build();
    }

    private void stubCurrentPage() {
        when(channelRepository.findByCode("RECOMMEND")).thenReturn(Optional.of(ExploreChannel.builder()
                .channelCode("RECOMMEND")
                .status(ExploreChannelStatus.ENABLED)
                .build()));
        when(channelRepository.findCurrentPublishedRevision("RECOMMEND"))
                .thenReturn(Optional.of(ExploreChannelRevision.builder()
                        .revisionId("ecr_01")
                        .channelCode("RECOMMEND")
                        .revisionNo(1)
                        .name("推荐")
                        .sortNo(10)
                        .status(ExploreChannelStatus.ENABLED)
                        .revisionStatus(ExploreRevisionStatus.PUBLISHED)
                        .build()));
        when(pageRepository.findCurrentPage("RECOMMEND")).thenReturn(Optional.of(ExplorePage.builder()
                .pageId("er_01")
                .channelCode("RECOMMEND")
                .status(ExplorePageStatus.PUBLISHED)
                .effectiveAt(NOW.minusSeconds(60))
                .build()));
    }

    private ExploreComponentData component(String componentId, ExploreComponentType type) {
        return new ExploreComponentData(ExplorePageComponent.builder()
                .componentId(componentId)
                .pageId("er_01")
                .componentType(type)
                .config(Map.of())
                .build(), List.of());
    }

    private ExploreImage image(String fileName) {
        return ExploreImage.builder()
                .url("https://assets.example/" + fileName)
                .width(1600)
                .height(900)
                .mime("image/png")
                .size(2048L)
                .build();
    }
}
