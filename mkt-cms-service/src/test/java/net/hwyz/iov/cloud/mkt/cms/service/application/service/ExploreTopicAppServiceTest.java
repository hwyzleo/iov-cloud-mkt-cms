package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreContentPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreTopicCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreTopicView;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreContentStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreRevisionStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreSourceType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreArticleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreOperationRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.ExploreTopicRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 专题跳转互斥语义与专题图集应用服务测试（MKT-CMS-DSN-CR-004 §3.1 / §5.1 / §9）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("专题跳转互斥语义与图集")
class ExploreTopicAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-27T10:00:00Z");

    private static final String TOPIC_ID = "tp_01";

    private static final String OPERATOR = "tester";

    @Mock
    private ExploreTopicRepository topicRepository;

    @Mock
    private ExploreArticleRepository articleRepository;

    @Mock
    private ExploreOperationRepository operationRepository;

    private ExploreTopicAppService topicAppService;

    @BeforeEach
    void setUp() {
        ExploreMediaValidator mediaValidator = new ExploreMediaValidator(
                Set.of("assets.example"), Set.of("https://h5.example"),
                Set.of("articleDetail"), Set.of("id"));
        ExploreTargetAppService targetAppService = new ExploreTargetAppService(mediaValidator,
                articleRepository, topicRepository);
        topicAppService = new ExploreTopicAppService(topicRepository, articleRepository,
                operationRepository, mediaValidator, targetAppService, Clock.fixed(NOW, ZoneOffset.UTC));
        stubWritableTopic();
    }

    @Test
    @DisplayName("专题详情模式允许图集与多篇文章并存")
    void 专题详情模式允许图文并存() {
        stubPublishedArticle("arv_1", "at_1");
        stubPublishedArticle("arv_2", "at_2");

        topicAppService.update(cmd(null, List.of(image("a.png"), image("b.png")),
                List.of("arv_1", "arv_2")));

        ExploreTopicRevision revision = captureInsertedRevision(1).get(0);
        assertNull(revision.getTarget(), "专题详情模式的存储目标必须为空");
        assertEquals(2, revision.getImages().size());
        assertEquals("https://assets.example/a.png", revision.getCover().getUrl(), "封面取首图");
        assertNotNull(revision.getContentHash());
        verify(topicRepository).replaceRevisionImages(revision.getRevisionId(), revision.getImages());
        verify(topicRepository).replaceItems(eq(revision.getRevisionId()), anyList());
    }

    @Test
    @DisplayName("文章详情模式保留目标且允许空文章列表")
    void 文章详情模式允许空列表() {
        stubPublishedArticle("arv_1", "at_1");

        topicAppService.update(cmd(articleTarget(), List.of(image("a.png")), List.of()));

        ExploreTopicRevision revision = captureInsertedRevision(1).get(0);
        assertNotNull(revision.getTarget());
        assertEquals(ExploreTargetType.ARTICLE, revision.getTarget().getType());
        assertEquals("at_1", revision.getTarget().getRefId());
        assertEquals("arv_1", revision.getTarget().getRevisionId());
    }

    @Test
    @DisplayName("文章详情/ H5 / 原生模式关联文章列表返回 105009")
    void 非专题详情模式禁止关联文章() {
        stubPublishedArticle("arv_1", "at_1");
        List<String> articles = List.of("arv_1");

        assertInvalid(() -> topicAppService.update(cmd(articleTarget(), List.of(image("a.png")), articles)));
        assertInvalid(() -> topicAppService.update(cmd(h5Target(), List.of(image("a.png")), articles)));
        assertInvalid(() -> topicAppService.update(cmd(nativeTarget(), List.of(image("a.png")), articles)));
    }

    @Test
    @DisplayName("指向其他专题的 TOPIC 目标返回 105009")
    void 其他专题目标被拒绝() {
        assertInvalid(() -> topicAppService.update(cmd(ExploreTarget.builder()
                .type(ExploreTargetType.TOPIC).refId("tp_other").revisionId("tr_other").build(),
                List.of(image("a.png")), List.of())));
    }

    @Test
    @DisplayName("历史遗留 TOPIC(self) 归一为专题详情模式并允许关联文章")
    void 兼容历史自身目标() {
        stubPublishedArticle("arv_1", "at_1");

        topicAppService.update(cmd(ExploreTarget.builder()
                .type(ExploreTargetType.TOPIC).refId(TOPIC_ID).revisionId("tr_legacy").build(),
                List.of(image("a.png")), List.of("arv_1")));

        ExploreTopicRevision revision = captureInsertedRevision(1).get(0);
        assertNull(revision.getTarget());
        assertEquals(1, revision.getImages().size());
    }

    @Test
    @DisplayName("图集超过 9 张返回 105009")
    void 图集上限() {
        List<ExploreImage> images = new ArrayList<>();
        for (int index = 1; index <= 10; index++) {
            images.add(image("p" + index + ".png"));
        }
        assertInvalid(() -> topicAppService.update(cmd(null, images, List.of())));
    }

    @Test
    @DisplayName("图集为空时封面为空；缺省 images 兼容旧单封面")
    void 图集与封面兼容() {
        topicAppService.update(cmd(null, List.of(), List.of()));
        assertNull(captureInsertedRevision(1).get(0).getCover());

        topicAppService.update(ExploreTopicCmd.builder()
                .topicId(TOPIC_ID).name("专题").cover(image("legacy.png"))
                .lockVersion(0).operator(OPERATOR).build());
        assertEquals(1, captureInsertedRevision(2).get(1).getImages().size());
        assertEquals("https://assets.example/legacy.png",
                captureInsertedRevision(2).get(1).getCover().getUrl());
    }

    @Test
    @DisplayName("contentHash 覆盖有序图集元数据")
    void 图集参与内容哈希() {
        topicAppService.update(cmd(null, List.of(image("a.png"), image("b.png")), List.of()));
        topicAppService.update(cmd(null, List.of(image("b.png"), image("a.png")), List.of()));

        List<ExploreTopicRevision> revisions = captureInsertedRevision(2);
        assertNotEquals(revisions.get(0).getContentHash(), revisions.get(1).getContentHash(),
                "图片顺序变化必须改变 contentHash");
    }

    @Test
    @DisplayName("showInDetail 缺省归一为 true，仅标志变化也改变 contentHash")
    void 详情可见标志归一与哈希() {
        topicAppService.update(cmd(null, List.of(image("a.png")), List.of()));
        topicAppService.update(cmd(null, List.of(imageWithDetail("a.png", false)), List.of()));

        List<ExploreTopicRevision> revisions = captureInsertedRevision(2);
        assertTrue(revisions.get(0).getImages().get(0).detailVisible(), "缺省应为详情可见");
        assertFalse(revisions.get(1).getImages().get(0).detailVisible(), "显式 false 应保留");
        assertNotEquals(revisions.get(0).getContentHash(), revisions.get(1).getContentHash(),
                "仅 showInDetail 变化必须改变 contentHash");
    }

    @Test
    @DisplayName("显式 showInDetail=true 与缺省 true 产生相同 contentHash")
    void 详情可见标志幂等哈希() {
        topicAppService.update(cmd(null, List.of(image("a.png")), List.of()));
        topicAppService.update(cmd(null, List.of(imageWithDetail("a.png", true)), List.of()));

        List<ExploreTopicRevision> revisions = captureInsertedRevision(2);
        assertEquals(revisions.get(0).getContentHash(), revisions.get(1).getContentHash());
    }

    @Test
    @DisplayName("发布门禁拦截历史非法草稿（非专题详情模式 + 已有文章条目）")
    void 发布门禁拦截非法草稿() {
        stubPublishedArticle("arv_1", "at_1");
        ExploreTopicRevision draft = ExploreTopicRevision.builder()
                .revisionId("tr_01")
                .topicId(TOPIC_ID)
                .revisionNo(1)
                .status(ExploreRevisionStatus.DRAFT)
                .name("专题")
                .target(articleTarget())
                .build();
        when(topicRepository.findLatestDraft(TOPIC_ID)).thenReturn(Optional.of(draft));
        when(topicRepository.findRevision("tr_01")).thenReturn(Optional.of(draft));
        when(topicRepository.findRevisionImages("tr_01")).thenReturn(List.of(image("a.png")));
        when(topicRepository.listItems("tr_01")).thenReturn(List.of(ExploreTopicItem.builder()
                .topicRevisionId("tr_01").articleRevisionId("arv_1").sortNo(10).build()));

        assertInvalid(() -> topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(TOPIC_ID)
                .operator(OPERATOR)
                .build()));
    }

    @Test
    @DisplayName("管理视图回显图集与布局")
    void 管理视图回显图集() {
        ExploreTopic topic = ExploreTopic.builder()
                .topicId(TOPIC_ID)
                .status(ExploreContentStatus.PUBLISHED)
                .currentRevisionId("tr_01")
                .lockVersion(2)
                .build();
        ExploreTopicRevision revision = ExploreTopicRevision.builder()
                .revisionId("tr_01")
                .topicId(TOPIC_ID)
                .revisionNo(1)
                .status(ExploreRevisionStatus.PUBLISHED)
                .name("专题")
                .build();
        when(topicRepository.findByTopicId(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(topicRepository.findRevision("tr_01")).thenReturn(Optional.of(revision));
        when(topicRepository.findRevisionImages("tr_01"))
                .thenReturn(List.of(image("a.png"), image("b.png"), image("c.png")));

        ExploreTopicView view = topicAppService.get(TOPIC_ID);

        assertEquals(3, view.getRevision().getImages().size());
        assertEquals("https://assets.example/a.png", view.getRevision().getCover().getUrl());
    }

    private void stubWritableTopic() {
        ExploreTopic topic = ExploreTopic.builder()
                .topicId(TOPIC_ID)
                .status(ExploreContentStatus.PUBLISHED)
                .lockVersion(0)
                .build();
        when(topicRepository.findByTopicIdForUpdate(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(topicRepository.findByTopicId(TOPIC_ID)).thenReturn(Optional.of(topic));
        when(topicRepository.findLatestDraft(anyString())).thenReturn(Optional.empty());
        when(topicRepository.findLatestRevision(anyString())).thenReturn(Optional.empty());
        when(topicRepository.findRevision(anyString())).thenReturn(Optional.empty());
        when(topicRepository.findRevisionImages(anyString())).thenReturn(List.of());
        when(topicRepository.listItems(anyString())).thenReturn(List.of());
        when(topicRepository.update(any(ExploreTopic.class), anyInt())).thenReturn(1);
    }

    private void stubPublishedArticle(String revisionId, String articleId) {
        when(articleRepository.findRevision(revisionId)).thenReturn(Optional.of(ExploreArticleRevision.builder()
                .revisionId(revisionId)
                .articleId(articleId)
                .revisionNo(1)
                .status(ExploreRevisionStatus.PUBLISHED)
                .title("文章")
                .summary("摘要")
                .displayAt(NOW)
                .createBy(OPERATOR)
                .createTime(NOW)
                .modifyBy(OPERATOR)
                .modifyTime(NOW)
                .build()));
        when(articleRepository.findByArticleId(articleId)).thenReturn(Optional.of(ExploreArticle.builder()
                .articleId(articleId)
                .sourceType(ExploreSourceType.OFFICIAL)
                .status(ExploreContentStatus.PUBLISHED)
                .lockVersion(0)
                .build()));
        when(articleRepository.findRevisionImages(revisionId)).thenReturn(List.of());
    }

    private List<ExploreTopicRevision> captureInsertedRevision(int times) {
        ArgumentCaptor<ExploreTopicRevision> captor = ArgumentCaptor.forClass(ExploreTopicRevision.class);
        verify(topicRepository, times(times)).insertRevision(captor.capture());
        return captor.getAllValues();
    }

    private void assertInvalid(Runnable action) {
        ExploreException exception = assertThrows(ExploreException.class, action::run);
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, exception.getErrorCode());
        assertEquals("105009", exception.getErrorCode().getCode());
    }

    private ExploreTopicCmd cmd(ExploreTarget target, List<ExploreImage> images, List<String> articleRevisionIds) {
        return ExploreTopicCmd.builder()
                .topicId(TOPIC_ID)
                .name("专题")
                .summary("摘要")
                .images(images)
                .target(target)
                .articleRevisionIds(articleRevisionIds)
                .lockVersion(0)
                .operator(OPERATOR)
                .build();
    }

    private ExploreTarget articleTarget() {
        return ExploreTarget.builder()
                .type(ExploreTargetType.ARTICLE)
                .refId("at_1")
                .revisionId("arv_1")
                .build();
    }

    private ExploreTarget h5Target() {
        return ExploreTarget.builder()
                .type(ExploreTargetType.H5)
                .url("https://h5.example/activity")
                .build();
    }

    private ExploreTarget nativeTarget() {
        return ExploreTarget.builder()
                .type(ExploreTargetType.NATIVE)
                .routeCode("articleDetail")
                .params(java.util.Map.of("id", "1"))
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

    private ExploreImage imageWithDetail(String fileName, boolean showInDetail) {
        return image(fileName).toBuilder().showInDetail(showInDetail).build();
    }
}
