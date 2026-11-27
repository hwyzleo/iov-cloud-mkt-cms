package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreItemType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreBannerRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExplorePageRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * 专题卡片解析与发布物化目标测试（MKT-CMS-DSN-CR-004 §4.1 / §4.2）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("专题卡片解析 - 目标与图集")
class ExplorePageContentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-27T10:00:00Z");

    @Mock
    private ExplorePageRepository pageRepository;

    @Mock
    private ExploreArticleRepository articleRepository;

    @Mock
    private ExploreTopicRepository topicRepository;

    @Mock
    private ExploreBannerRepository bannerRepository;

    private ExplorePageContentService contentService;

    @BeforeEach
    void setUp() {
        ExploreMediaValidator mediaValidator = new ExploreMediaValidator(
                Set.of("assets.example"), Set.of("https://h5.example"),
                Set.of("articleDetail"), Set.of("id"));
        ExploreTargetAppService targetAppService = new ExploreTargetAppService(mediaValidator,
                articleRepository, topicRepository);
        contentService = new ExplorePageContentService(pageRepository, articleRepository,
                topicRepository, bannerRepository, targetAppService);
    }

    @Test
    @DisplayName("专题详情模式：卡片目标为专题自身且携带图集布局")
    void 专题详情模式解析() {
        stubTopic(topic(null));
        when(topicRepository.findRevisionImages("tr_01"))
                .thenReturn(List.of(image("a.png"), image("b.png")));

        ExplorePageContentService.Resolved resolved = contentService.resolve(
                ExploreItemType.TOPIC, "tp_01", "tr_01", true);

        assertEquals(ExploreTargetType.TOPIC, resolved.target().getType());
        assertEquals("tp_01", resolved.target().getRefId());
        assertEquals("tr_01", resolved.target().getRevisionId());

        Map<String, Object> snapshot = resolved.snapshot();
        assertEquals(2, snapshot.get("imageCount"));
        assertEquals("TWO_COLUMNS", snapshot.get("imageLayout"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cover = (Map<String, Object>) snapshot.get("cover");
        assertEquals("https://assets.example/a.png", cover.get("url"));
        @SuppressWarnings("unchecked")
        Map<String, Object> target = (Map<String, Object>) snapshot.get("target");
        assertEquals("TOPIC", target.get("type"));
        assertEquals("tr_01", target.get("revisionId"));
    }

    @Test
    @DisplayName("文章详情模式：卡片目标透传到条目与展示快照")
    void 文章详情模式解析() {
        ExploreTarget target = ExploreTarget.builder()
                .type(ExploreTargetType.ARTICLE)
                .refId("at_01")
                .revisionId("arv_01")
                .build();
        stubTopic(topic(target));
        stubPublishedArticle();
        when(topicRepository.findRevisionImages("tr_01")).thenReturn(List.of());

        ExplorePageContentService.Resolved resolved = contentService.resolve(
                ExploreItemType.TOPIC, "tp_01", "tr_01", true);

        assertEquals(ExploreTargetType.ARTICLE, resolved.target().getType());
        assertEquals("at_01", resolved.target().getRefId());
        assertEquals("arv_01", resolved.target().getRevisionId());
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshotTarget = (Map<String, Object>) resolved.snapshot().get("target");
        assertEquals("ARTICLE", snapshotTarget.get("type"));
        assertEquals("arv_01", snapshotTarget.get("revisionId"));
    }

    @Test
    @DisplayName("历史 TOPIC(self) 目标解析为专题详情模式")
    void 历史自身目标解析() {
        ExploreTarget legacy = ExploreTarget.builder()
                .type(ExploreTargetType.TOPIC)
                .refId("tp_01")
                .revisionId("tr_01")
                .build();
        stubTopic(topic(legacy));
        when(topicRepository.findRevisionImages("tr_01")).thenReturn(List.of(image("a.png")));

        ExplorePageContentService.Resolved resolved = contentService.resolve(
                ExploreItemType.TOPIC, "tp_01", "tr_01", true);

        assertEquals(ExploreTargetType.TOPIC, resolved.target().getType());
        assertEquals("tp_01", resolved.target().getRefId());
        assertEquals(1, resolved.snapshot().get("imageCount"));
        assertNotNull(resolved.snapshot().get("target"));
    }

    private void stubTopic(ExploreTopicRevision revision) {
        when(topicRepository.findByTopicId("tp_01")).thenReturn(Optional.of(ExploreTopic.builder()
                .topicId("tp_01")
                .status(ExploreContentStatus.PUBLISHED)
                .currentRevisionId("tr_01")
                .lockVersion(1)
                .build()));
        when(topicRepository.findRevision("tr_01")).thenReturn(Optional.of(revision));
    }

    private void stubPublishedArticle() {
        when(articleRepository.findRevision("arv_01")).thenReturn(Optional.of(ExploreArticleRevision.builder()
                .revisionId("arv_01")
                .articleId("at_01")
                .revisionNo(1)
                .status(ExploreRevisionStatus.PUBLISHED)
                .title("文章")
                .displayAt(NOW)
                .build()));
        when(articleRepository.findByArticleId("at_01")).thenReturn(Optional.of(ExploreArticle.builder()
                .articleId("at_01")
                .status(ExploreContentStatus.PUBLISHED)
                .lockVersion(0)
                .build()));
    }

    private ExploreTopicRevision topic(ExploreTarget target) {
        return ExploreTopicRevision.builder()
                .revisionId("tr_01")
                .topicId("tp_01")
                .revisionNo(1)
                .status(ExploreRevisionStatus.PUBLISHED)
                .name("专题")
                .summary("摘要")
                .target(target)
                .build();
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
