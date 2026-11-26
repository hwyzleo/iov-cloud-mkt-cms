package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreComponentData;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreChannelStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
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

    private void stubCurrentPage() {
        when(channelRepository.findByCode("RECOMMEND")).thenReturn(Optional.of(ExploreChannel.builder()
                .channelCode("RECOMMEND")
                .status(ExploreChannelStatus.ENABLED)
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
}
