package net.hwyz.iov.cloud.mkt.cms.service.it;

import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration;
import net.hwyz.iov.cloud.framework.mysql.config.MyBatisMetaObjectHandler;
import net.hwyz.iov.cloud.framework.mysql.config.MyBatisPlusConfig;
import net.hwyz.iov.cloud.mkt.cms.service.CmsApplication;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreArticleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreBannerCmd;
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
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreArticleAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreBannerAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreMobileQueryAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExplorePageAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExplorePublishAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreScheduleTxService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreTopicAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreItemType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePageStatus;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreComponentView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageView;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 探索页子域真实 MySQL 集成测试（MKT-CMS-DSN-CR-003 §7 / AT-013～AT-017）。
 * <p>
 * 执行方式（需要本地或 CI MySQL）：
 * <pre>
 * mvn test -Dit.mysql.password=&lt;password&gt; [-Dit.mysql.username=root] \
 *          [-Dit.mysql.host=127.0.0.1] [-Dit.mysql.db=db_cms]
 * </pre>
 * 未提供密码时显式跳过（不伪造通过）。调度使用可控数据库时间 + 直接调用调度事务方法，
 * 不依赖真实等待；页面发布使用真实事务、行锁与唯一键。
 *
 * @author hwyz_leo
 */
@EnabledIfSystemProperty(named = "it.mysql.password", matches = ".+")
@SpringBootTest(classes = ExploreFlowIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.datasource.url=jdbc:mysql://${it.mysql.host:127.0.0.1}:3306/${it.mysql.db:db_cms_explore_it}"
                        + "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
                        + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
                        + "&forceConnectionTimeZoneToSession=true&preserveInstants=true&createDatabaseIfNotExist=true",
                "spring.datasource.username=${it.mysql.username:root}",
                "spring.datasource.password=${it.mysql.password:}",
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0",
                "spring.flyway.validate-on-migrate=true",
                "iov.web.jackson.zone-id=UTC",
                "iov.mysql.mapper-validation.mode=fail",
                "mybatis-plus.mapper-locations=classpath*:mappers/*.xml",
                "cms.explore.scheduler-enabled=false",
                "cms.explore.allowed-asset-hosts=assets.example",
                "cms.explore.allowed-h5-origins=https://h5.example",
                "cms.explore.native-route-codes=articleDetail",
                "cms.explore.native-param-keys=id"
        })
class ExploreFlowIT {

    private static final String OPERATOR = "tester";

    private static final String CHANNEL = "RECOMMEND";

    @Autowired
    private ExploreArticleAppService articleAppService;
    @Autowired
    private ExploreTopicAppService topicAppService;
    @Autowired
    private ExploreBannerAppService bannerAppService;
    @Autowired
    private ExplorePageAppService pageAppService;
    @Autowired
    private ExplorePublishAppService publishAppService;
    @Autowired
    private ExploreScheduleTxService scheduleTxService;
    @Autowired
    private ExploreMobileQueryAppService mobileQueryAppService;
    @MockBean
    private ImageService imageService;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM cms_explore_page_item");
        jdbcTemplate.update("DELETE FROM cms_explore_page_component");
        jdbcTemplate.update("DELETE FROM cms_explore_page_current");
        jdbcTemplate.update("DELETE FROM cms_explore_page");
        jdbcTemplate.update("DELETE FROM cms_explore_topic_image");
        jdbcTemplate.update("DELETE FROM cms_explore_topic_item");
        jdbcTemplate.update("DELETE FROM cms_explore_topic_revision");
        jdbcTemplate.update("DELETE FROM cms_explore_topic");
        jdbcTemplate.update("DELETE FROM cms_explore_banner_revision");
        jdbcTemplate.update("DELETE FROM cms_explore_banner");
        jdbcTemplate.update("DELETE FROM cms_explore_article_revision");
        jdbcTemplate.update("DELETE FROM cms_explore_article");
        jdbcTemplate.update("DELETE FROM cms_explore_operation");
        jdbcTemplate.update("UPDATE cms_explore_channel SET status = 'ENABLED'");
    }

    @Test
    @DisplayName("AT-013：V1.1.0 建表、索引与 V1.1.1 三频道预置")
    void 迁移与预置频道() throws Exception {
        for (String table : new String[]{"cms_explore_channel", "cms_explore_article",
                "cms_explore_article_revision", "cms_explore_topic", "cms_explore_topic_revision",
                "cms_explore_topic_item", "cms_explore_banner", "cms_explore_banner_revision",
                "cms_explore_page", "cms_explore_page_component", "cms_explore_page_item",
                "cms_explore_page_current", "cms_explore_article_image", "cms_explore_topic_image",
                "cms_explore_operation"}) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() "
                                 + "AND table_name = ?")) {
                statement.setString(1, table);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertEquals(1, resultSet.getInt(1), "缺少表: " + table);
                }
            }
        }
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel WHERE channel_code IN"
                        + " ('RECOMMEND','ACTIVITY','LOCAL')", Integer.class));
        assertEquals(1, countIndex("cms_explore_page", "uk_page_channel_version"));
        assertEquals(1, countIndex("cms_explore_page", "uk_page_request_id"));
        assertEquals(1, countIndex("cms_explore_page_item", "idx_item_cursor"));
        assertEquals(1, countIndex("cms_explore_article_image", "uk_article_image_sort"));
        assertEquals(1, countIndex("cms_explore_topic_image", "uk_topic_image_sort"));
        assertEquals(0, countIndex("cms_explore_page_component", "uk_component_page_type"));
    }

    @Test
    @DisplayName("AT-013/AT-018：官方富文本清洗、不可变修订、UGC 引用与乐观锁")
    void 内容生命周期与安全() {
        ExploreArticleView created = articleAppService.create(ExploreArticleCmd.builder()
                .sourceType("OFFICIAL")
                .title("官方文章")
                .summary("摘要")
                .body("<p>正文</p><script>alert(1)</script><img src=\"https://assets.example/a.webp\">")
                .operator(OPERATOR)
                .build());
        assertEquals("<p>正文</p><img src=\"https://assets.example/a.webp\">",
                created.getRevision().getBody());
        assertNotNull(created.getRevision().getContentHash());

        ExploreArticleView updated = articleAppService.update(ExploreArticleCmd.builder()
                .articleId(created.getArticle().getArticleId())
                .sourceType("OFFICIAL")
                .title("官方文章 v2")
                .body("<h2>新正文</h2>")
                .lockVersion(0)
                .operator(OPERATOR)
                .build());
        assertEquals(1, updated.getArticle().getLockVersion());
        assertEquals(1, updated.getRevision().getRevisionNo());

        ExploreException conflict = assertThrows(ExploreException.class,
                () -> articleAppService.update(ExploreArticleCmd.builder()
                        .articleId(created.getArticle().getArticleId())
                        .sourceType("OFFICIAL")
                        .title("过期覆盖")
                        .body("<p>旧</p>")
                        .lockVersion(0)
                        .operator(OPERATOR)
                        .build()));
        assertEquals(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, conflict.getErrorCode());

        ExploreArticleView published = articleAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(created.getArticle().getArticleId())
                .operator(OPERATOR)
                .build());
        assertEquals("<h2>新正文</h2>", published.getRevision().getBody());
        assertEquals(published.getRevision().getRevisionId(),
                published.getArticle().getCurrentRevisionId());

        ExploreArticleView ugc = articleAppService.create(ExploreArticleCmd.builder()
                .sourceType("UGC_REFERENCE")
                .title("用户内容引用")
                .summary("展示快照")
                .authorName("用户A")
                .sourceContentId("uc_001")
                .nativeRouteCode("articleDetail")
                .operator(OPERATOR)
                .build());
        ExploreArticleView ugcPublished = articleAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(ugc.getArticle().getArticleId())
                .operator(OPERATOR)
                .build());
        Map<String, Object> ugcDetail = mobileQueryAppService.articleDetail(
                ugc.getArticle().getArticleId(), ugcPublished.getRevision().getRevisionId());
        assertEquals("uc_001", ugcDetail.get("sourceContentId"));
        assertEquals("articleDetail", ugcDetail.get("nativeRouteCode"));
        assertFalse(ugcDetail.containsKey("body"));

        articleAppService.offline(ExploreContentPublishCmd.builder()
                .objectId(ugc.getArticle().getArticleId()).operator(OPERATOR).build());
        ExploreException offline = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.articleDetail(
                        ugc.getArticle().getArticleId(), ugcPublished.getRevision().getRevisionId()));
        assertEquals(ExploreErrorCode.EXPLORE_CONTENT_NOT_FOUND, offline.getErrorCode());
    }

    @Test
    @DisplayName("AT-014/AT-016：页面编排、发布、手机当前页、keyset 分页与文章/专题详情")
    void 页面发布与手机读取() {
        List<ExploreArticleView> articles = new ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            articles.add(publishedArticle("文章" + index, "<p>正文" + index + "</p>"));
        }
        ExploreTopicView topic = publishedTopic("专题一", articles.get(0));
        ExploreBannerView banner = publishedBanner(articles.get(0));

        savePage(0, List.of(
                bannerComponent(banner),
                topicComponent(topic),
                feedComponent(articles)));

        ExplorePublishResult published = publishPage("req-page-1", null,
                "IMMEDIATE", null, null);
        assertEquals(ExplorePageStatus.PUBLISHED.name(), published.getPage().getStatus().name());

        ExplorePageView page = mobileQueryAppService.currentPage(CHANNEL, 2);
        assertEquals(published.getPage().getPageId(), page.getReleaseId());
        assertEquals(3, page.getComponents().size());
        ExploreComponentView feed = page.getComponents().stream()
                .filter(component -> "ARTICLE_FEED".equals(component.getType()))
                .findFirst().orElseThrow();
        assertEquals(2, feed.getItems().size());
        assertTrue(feed.isHasMore());
        assertNotNull(feed.getNextCursor());

        Map<String, Object> next = mobileQueryAppService.articleFeed(CHANNEL,
                feed.getComponentId(), page.getReleaseId(), feed.getNextCursor(), 2);
        assertEquals(1, ((List<?>) next.get("items")).size());
        assertEquals(false, next.get("hasMore"));

        Map<String, Object> articleDetail = mobileQueryAppService.articleDetail(
                articles.get(0).getArticle().getArticleId(),
                articles.get(0).getRevision().getRevisionId());
        assertEquals("<p>正文1</p>", articleDetail.get("body"));

        Map<String, Object> topicDetail = mobileQueryAppService.topicDetail(
                topic.getTopic().getTopicId(), topic.getRevision().getRevisionId(), null, 10);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topicItems = (List<Map<String, Object>>) topicDetail.get("items");
        assertEquals(1, topicItems.size());
        assertEquals("专题一", topicDetail.get("name"));
        assertEquals(articles.get(0).getRevision().getRevisionId(), topicItems.get(0).get("itemId"));
        assertEquals("ARTICLE", topicItems.get(0).get("itemType"));
        assertEquals(10, topicItems.get(0).get("sortNo"));

        ExploreException changed = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.articleFeed(CHANNEL, feed.getComponentId(),
                        "er_missing", null, 10));
        assertEquals(ExploreErrorCode.EXPLORE_RELEASE_CHANGED, changed.getErrorCode());
    }

    @Test
    @DisplayName("AT-015：同 requestId 幂等、不同命令冲突、旧 expectedReleaseId 拒绝")
    void 发布幂等与前提冲突() {
        ExploreArticleView article = publishedArticle("文章", "<p>正文</p>");
        savePage(0, List.of(feedComponent(List.of(article))));
        ExplorePublishResult first = publishPage("req-1", null, "IMMEDIATE", null, null);

        ExplorePublishResult replay = publishPage("req-1", null, "IMMEDIATE", null, null);
        assertEquals(first.getPage().getPageId(), replay.getPage().getPageId());

        ExploreException idempotency = assertThrows(ExploreException.class,
                () -> publishPage("req-1", null, "IMMEDIATE", null,
                        Instant.now().plus(3600, ChronoUnit.SECONDS)));
        assertEquals(ExploreErrorCode.EXPLORE_IDEMPOTENCY_CONFLICT, idempotency.getErrorCode());

        ExploreException stale = assertThrows(ExploreException.class,
                () -> publishPage("req-2", "er_old", "IMMEDIATE", null, null));
        assertEquals(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT, stale.getErrorCode());

        ExploreException futureImmediate = assertThrows(ExploreException.class,
                () -> publishPage("req-3", first.getPage().getPageId(), "IMMEDIATE",
                        Instant.now().plus(600, ChronoUnit.SECONDS), null));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, futureImmediate.getErrorCode());
    }

    @Test
    @DisplayName("AT-015：定时发布唯一待生效、激活切换、到期清空不回退")
    void 定时生效与到期() {
        ExploreArticleView article = publishedArticle("文章", "<p>正文</p>");
        savePage(0, List.of(feedComponent(List.of(article))));
        ExplorePublishResult immediate = publishPage("req-immediate", null, "IMMEDIATE", null, null);

        ExplorePageData draft = pageAppService.getDraft(CHANNEL);
        ExplorePagePublishCmd scheduledCmd = ExplorePagePublishCmd.builder()
                .channelCode(CHANNEL)
                .requestId("req-scheduled")
                .expectedReleaseId(immediate.getPage().getPageId())
                .publishType("SCHEDULED")
                .effectiveAt(Instant.now().plus(3600, ChronoUnit.SECONDS))
                .operator(OPERATOR)
                .build();
        ExplorePublishResult scheduled = publishAppService.publish(scheduledCmd);
        assertEquals(ExplorePageStatus.SCHEDULED.name(), scheduled.getPage().getStatus().name());

        ExploreException secondSchedule = assertThrows(ExploreException.class,
                () -> publishPage("req-scheduled-2", immediate.getPage().getPageId(), "SCHEDULED",
                        Instant.now().plus(7200, ChronoUnit.SECONDS), null));
        assertEquals(ExploreErrorCode.EXPLORE_SCHEDULE_CONFLICT, secondSchedule.getErrorCode());

        assertEquals(immediate.getPage().getPageId(),
                mobileQueryAppService.currentPage(CHANNEL, 10).getReleaseId());

        jdbcTemplate.update("UPDATE cms_explore_page SET effective_at = DATE_SUB(UTC_TIMESTAMP(3),"
                + " INTERVAL 1 MINUTE) WHERE page_id = ?", scheduled.getPage().getPageId());
        scheduleTxService.activate(scheduled.getPage().getPageId());
        assertEquals(scheduled.getPage().getPageId(),
                mobileQueryAppService.currentPage(CHANNEL, 10).getReleaseId());

        jdbcTemplate.update("UPDATE cms_explore_page SET expire_at = DATE_SUB(UTC_TIMESTAMP(3),"
                + " INTERVAL 1 SECOND) WHERE page_id = ?", scheduled.getPage().getPageId());
        scheduleTxService.expire(scheduled.getPage().getPageId());
        ExploreException noPage = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.currentPage(CHANNEL, 10));
        assertEquals(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, noPage.getErrorCode());
    }

    @Test
    @DisplayName("AT-015/AT-016：内容下线实时过滤、页面立即下线取消待生效版本")
    void 下线与实时过滤() {
        List<ExploreArticleView> articles = new ArrayList<>();
        for (int index = 1; index <= 2; index++) {
            articles.add(publishedArticle("文章" + index, "<p>正文" + index + "</p>"));
        }
        savePage(0, List.of(feedComponent(articles)));
        ExplorePublishResult first = publishPage("req-offline-1", null, "IMMEDIATE", null, null);

        ExplorePagePublishCmd scheduled = ExplorePagePublishCmd.builder()
                .channelCode(CHANNEL)
                .requestId("req-offline-scheduled")
                .expectedReleaseId(first.getPage().getPageId())
                .publishType("SCHEDULED")
                .effectiveAt(Instant.now().plus(3600, ChronoUnit.SECONDS))
                .operator(OPERATOR)
                .build();
        ExplorePublishResult pending = publishAppService.publish(scheduled);

        articleAppService.offline(ExploreContentPublishCmd.builder()
                .objectId(articles.get(0).getArticle().getArticleId()).operator(OPERATOR).build());
        ExplorePageView page = mobileQueryAppService.currentPage(CHANNEL, 10);
        ExploreComponentView feed = page.getComponents().get(0);
        assertEquals(1, feed.getItems().size());
        assertNotEquals(articles.get(0).getArticle().getArticleId(),
                feed.getItems().get(0).get("articleId"));

        publishAppService.offline(CHANNEL, OPERATOR);
        ExploreException noPage = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.currentPage(CHANNEL, 10));
        assertEquals(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, noPage.getErrorCode());
        assertEquals(ExplorePageStatus.CANCELED.name(), jdbcTemplate.queryForObject(
                "SELECT status FROM cms_explore_page WHERE page_id = ?", String.class,
                pending.getPage().getPageId()));
    }

    @Test
    @DisplayName("AT-015：历史版本复制为新草稿，不修改历史与当前版本")
    void 历史复制为新草稿() {
        ExploreArticleView article = publishedArticle("文章", "<p>正文</p>");
        savePage(0, List.of(feedComponent(List.of(article))));
        ExplorePublishResult release = publishPage("req-copy", null, "IMMEDIATE", null, null);
        int componentCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_page_component", Integer.class);

        ExplorePageData copied = pageAppService.copyToDraft(release.getPage().getPageId(), OPERATOR);
        assertNotNull(copied.getPage());
        assertEquals(ExplorePageStatus.DRAFT, copied.getPage().getStatus());
        assertNotEquals(release.getPage().getPageId(), copied.getPage().getPageId());
        assertEquals(componentCountBefore, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_page_component", Integer.class));
        assertEquals(release.getPage().getPageId(),
                mobileQueryAppService.currentPage(CHANNEL, 10).getReleaseId());
        assertEquals(ExplorePageStatus.PUBLISHED.name(), jdbcTemplate.queryForObject(
                "SELECT status FROM cms_explore_page WHERE page_id = ?", String.class,
                release.getPage().getPageId()));
    }

    @Test
    @DisplayName("AT-017：管理列表数据库物理分页并保留总数")
    void 管理检索物理分页() {
        for (int index = 1; index <= 3; index++) {
            publishedArticle("分页文章" + index, "<p>正文" + index + "</p>");
        }
        PageHelper.startPage(1, 2);
        List<ExploreArticleView> rows = articleAppService.search(
                net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleSearchCriteria.builder().build());
        try {
            assertTrue(rows instanceof Page);
            assertEquals(2, rows.size());
            assertEquals(3, ((Page<?>) rows).getTotal());
        } finally {
            PageHelper.clearPage();
        }
    }

    @Test
    @DisplayName("文章多图：最多 9 张、返回数量与布局建议")
    void 文章多图() {
        ExploreArticleView article = articleAppService.create(ExploreArticleCmd.builder()
                .sourceType("OFFICIAL")
                .title("多图文章")
                .summary("多图摘要")
                .images(List.of(testImage("https://assets.example/1.jpg", "image/jpeg"),
                        testImage("https://assets.example/2.jpg", "image/jpeg")))
                .body("<p>多图正文</p>")
                .operator(OPERATOR)
                .build());
        ExploreArticleView published = articleAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(article.getArticle().getArticleId()).operator(OPERATOR).build());

        Map<String, Object> detail = mobileQueryAppService.articleDetail(
                article.getArticle().getArticleId(), published.getRevision().getRevisionId());
        assertEquals(2, detail.get("imageCount"));
        assertEquals("TWO_COLUMNS", detail.get("imageLayout"));
        assertEquals(2, ((List<?>) detail.get("images")).size());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_article_image WHERE article_revision_id = ?",
                Integer.class, published.getRevision().getRevisionId()));

        ExploreException tooMany = assertThrows(ExploreException.class,
                () -> articleAppService.create(ExploreArticleCmd.builder()
                        .sourceType("OFFICIAL")
                        .title("超过九图")
                        .body("<p>正文</p>")
                        .images(java.util.stream.IntStream.range(1, 11)
                                .mapToObj(index -> testImage("https://assets.example/" + index + ".jpg",
                                        "image/jpeg"))
                                .toList())
                        .operator(OPERATOR)
                        .build()));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, tooMany.getErrorCode());
    }

    @Test
    @DisplayName("同类组件可多次添加，专题样式及多文章流独立翻页生效")
    void 同类组件多次与专题样式() {
        ExploreArticleView article = publishedArticle("多组件文章", "<p>正文</p>");
        ExploreArticleView secondFeedArticleOne = publishedArticle("第二文章流一", "<p>正文一</p>");
        ExploreArticleView secondFeedArticleTwo = publishedArticle("第二文章流二", "<p>正文二</p>");
        ExploreTopicView topic = publishedTopic("多组件专题", article);

        ExplorePageSaveCmd.Component topicScroll = ExplorePageSaveCmd.Component.builder()
                .type(ExploreComponentType.TOPIC_GRID.name())
                .sortNo(10)
                .config(Map.of("title", "横滑专题", "style", "HORIZONTAL_SCROLL"))
                .items(List.of(item(ExploreItemType.TOPIC, topic.getTopic().getTopicId(),
                        topic.getRevision().getRevisionId(), 10)))
                .build();
        ExplorePageSaveCmd.Component topicText = ExplorePageSaveCmd.Component.builder()
                .type(ExploreComponentType.TOPIC_GRID.name())
                .sortNo(20)
                .config(Map.of("title", "图文专题", "style", "IMAGE_TEXT_LIST"))
                .items(List.of(item(ExploreItemType.TOPIC, topic.getTopic().getTopicId(),
                        topic.getRevision().getRevisionId(), 10)))
                .build();
        ExplorePageSaveCmd.Component feedOne = ExplorePageSaveCmd.Component.builder()
                .type(ExploreComponentType.ARTICLE_FEED.name())
                .sortNo(30)
                .config(Map.of("title", "推荐阅读"))
                .items(List.of(item(ExploreItemType.ARTICLE, article.getArticle().getArticleId(),
                        article.getRevision().getRevisionId(), 10)))
                .build();
        ExplorePageSaveCmd.Component feedTwo = ExplorePageSaveCmd.Component.builder()
                .type(ExploreComponentType.ARTICLE_FEED.name())
                .sortNo(40)
                .config(Map.of("title", "更多内容"))
                .items(List.of(
                        item(ExploreItemType.ARTICLE, secondFeedArticleOne.getArticle().getArticleId(),
                                secondFeedArticleOne.getRevision().getRevisionId(), 10),
                        item(ExploreItemType.ARTICLE, secondFeedArticleTwo.getArticle().getArticleId(),
                                secondFeedArticleTwo.getRevision().getRevisionId(), 20)))
                .build();
        savePage(0, List.of(topicScroll, topicText, feedOne, feedTwo));
        publishPage("req-multi-components", null, "IMMEDIATE", null, null);

        ExplorePageView page = mobileQueryAppService.currentPage(CHANNEL, 1);
        assertEquals(4, page.getComponents().size());
        List<ExploreComponentView> topics = page.getComponents().stream()
                .filter(component -> "TOPIC_GRID".equals(component.getType())).toList();
        assertEquals(2, topics.size());
        assertEquals("HORIZONTAL_SCROLL", topics.get(0).getConfig().get("style"));
        assertEquals("横滑专题", topics.get(0).getConfig().get("title"));
        assertEquals("IMAGE_TEXT_LIST", topics.get(1).getConfig().get("style"));
        List<ExploreComponentView> feeds = page.getComponents().stream()
                .filter(component -> "ARTICLE_FEED".equals(component.getType())).toList();
        assertEquals(2, feeds.size());
        ExploreComponentView secondFeed = feeds.get(1);
        assertTrue(secondFeed.isHasMore());
        assertNotNull(secondFeed.getNextCursor());

        Map<String, Object> secondFeedNextPage = mobileQueryAppService.articleFeed(CHANNEL,
                secondFeed.getComponentId(), page.getReleaseId(), secondFeed.getNextCursor(), 1);
        assertEquals(secondFeed.getComponentId(), secondFeedNextPage.get("componentId"));
        assertEquals(1, ((List<?>) secondFeedNextPage.get("items")).size());
        assertFalse((Boolean) secondFeedNextPage.get("hasMore"));

        ExploreException invalidComponent = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.articleFeed(CHANNEL, topics.get(0).getComponentId(),
                        page.getReleaseId(), null, 1));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, invalidComponent.getErrorCode());
        assertEquals(4, pageAppService.getDraft(CHANNEL).getComponents().size());
    }

    @Test
    @DisplayName("AT-014：页面草稿乐观锁冲突不静默覆盖")
    void 页面草稿乐观锁() {
        ExploreArticleView article = publishedArticle("文章", "<p>正文</p>");
        ExplorePageData first = savePage(0, List.of(feedComponent(List.of(article))));
        assertEquals(1, first.getPage().getLockVersion());
        ExploreException conflict = assertThrows(ExploreException.class,
                () -> savePage(0, List.of(feedComponent(List.of(article)))));
        assertEquals(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, conflict.getErrorCode());
    }

    @Test
    @DisplayName("AT-028/AT-029：专题封面独立、详情可见子集与旧页面快照不变")
    void 专题详情可见子集与页面快照() {
        ExploreArticleView article = publishedArticle("详情图集文章", "<p>正文</p>");
        ExploreImage coverOnly = testImage("https://assets.example/cover.png", "image/png")
                .toBuilder().showInDetail(false).build();
        ExploreImage detail = testImage("https://assets.example/detail.png", "image/png")
                .toBuilder().showInDetail(true).build();
        ExploreTopicView created = topicAppService.create(ExploreTopicCmd.builder()
                .name("详情子集专题")
                .summary("摘要")
                .images(List.of(coverOnly, detail))
                .articleRevisionIds(List.of(article.getRevision().getRevisionId()))
                .operator(OPERATOR)
                .build());
        ExploreTopicView published = topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(created.getTopic().getTopicId()).operator(OPERATOR).build());
        String topicId = published.getTopic().getTopicId();

        savePage(0, List.of(topicComponent(published)));
        publishPage("req-topic-detail-1", null, "IMMEDIATE", null, null);

        Map<String, Object> snapshot = topicItemSnapshot();
        assertEquals(1, snapshot.get("imageCount"));
        assertEquals("SINGLE", snapshot.get("imageLayout"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cover = (Map<String, Object>) snapshot.get("cover");
        assertEquals("https://assets.example/cover.png", cover.get("url"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> images = (List<Map<String, Object>>) snapshot.get("images");
        assertEquals(1, images.size());
        assertEquals("https://assets.example/detail.png", images.get(0).get("url"));

        ExploreTopicView admin = topicAppService.get(topicId);
        assertEquals(2, admin.getRevision().getImages().size());
        assertFalse(admin.getRevision().getImages().get(0).detailVisible());

        ExploreTopicView nextDraft = topicAppService.update(ExploreTopicCmd.builder()
                .topicId(topicId)
                .name("详情子集专题")
                .summary("摘要")
                .images(List.of(testImage("https://assets.example/only.png", "image/png")))
                .articleRevisionIds(List.of(article.getRevision().getRevisionId()))
                .lockVersion(published.getTopic().getLockVersion())
                .operator(OPERATOR)
                .build());
        ExploreTopicView nextPublished = topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(topicId).operator(OPERATOR).build());
        assertEquals(nextDraft.getRevision().getRevisionId(),
                nextPublished.getRevision().getRevisionId());

        Map<String, Object> unchanged = topicItemSnapshot();
        assertEquals(1, unchanged.get("imageCount"), "旧页面发布快照不得被新专题修订改写");
        @SuppressWarnings("unchecked")
        Map<String, Object> unchangedCover = (Map<String, Object>) unchanged.get("cover");
        assertEquals("https://assets.example/cover.png", unchangedCover.get("url"));

        Map<String, Object> newDetail = mobileQueryAppService.topicDetail(topicId,
                nextPublished.getRevision().getRevisionId(), null, 10);
        assertEquals(1, newDetail.get("imageCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> newImages = (List<Map<String, Object>>) newDetail.get("images");
        assertEquals("https://assets.example/only.png", newImages.get(0).get("url"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> topicItemSnapshot() {
        ExplorePageView page = mobileQueryAppService.currentPage(CHANNEL, 10);
        ExploreComponentView topics = page.getComponents().stream()
                .filter(component -> "TOPIC_GRID".equals(component.getType())).findFirst().orElseThrow();
        return (Map<String, Object>) topics.getItems().get(0);
    }

    private ExploreImage testImage(String url, String mime) {
        return ExploreImage.builder()
                .url(url)
                .width(1200)
                .height(800)
                .mime(mime)
                .size(1024L)
                .build();
    }

    private ExploreArticleView publishedArticle(String title, String body) {
        ExploreArticleView created = articleAppService.create(ExploreArticleCmd.builder()
                .sourceType("OFFICIAL")
                .title(title)
                .summary(title + "摘要")
                .body(body)
                .operator(OPERATOR)
                .build());
        return articleAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(created.getArticle().getArticleId())
                .operator(OPERATOR)
                .build());
    }

    private ExploreTopicView publishedTopic(String name, ExploreArticleView article) {
        ExploreTopicView created = topicAppService.create(ExploreTopicCmd.builder()
                .name(name)
                .summary(name + "摘要")
                .articleRevisionIds(List.of(article.getRevision().getRevisionId()))
                .operator(OPERATOR)
                .build());
        return topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(created.getTopic().getTopicId())
                .operator(OPERATOR)
                .build());
    }

    private ExploreTarget target(ExploreTargetType type, String refId, String revisionId) {
        return ExploreTarget.builder()
                .type(type)
                .refId(refId)
                .revisionId(revisionId)
                .build();
    }

    @Test
    @DisplayName("AT-022：专题图集保存、回显与草稿替换事务")
    void 专题图集() {
        ExploreArticleView article = publishedArticle("专题图集文章", "<p>正文</p>");
        ExploreTopicView created = topicAppService.create(ExploreTopicCmd.builder()
                .name("图集专题")
                .summary("图集摘要")
                .images(List.of(testImage("https://assets.example/t1.png", "image/png"),
                        testImage("https://assets.example/t2.png", "image/png")))
                .articleRevisionIds(List.of(article.getRevision().getRevisionId()))
                .operator(OPERATOR)
                .build());
        assertEquals(2, created.getRevision().getImages().size());
        assertEquals("https://assets.example/t1.png", created.getRevision().getCover().getUrl());

        ExploreTopicView published = topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(created.getTopic().getTopicId()).operator(OPERATOR).build());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_topic_image WHERE topic_revision_id = ?",
                Integer.class, published.getRevision().getRevisionId()));

        Map<String, Object> detail = mobileQueryAppService.topicDetail(
                published.getTopic().getTopicId(), published.getRevision().getRevisionId(), null, 10);
        assertEquals(2, detail.get("imageCount"));
        assertEquals("TWO_COLUMNS", detail.get("imageLayout"));
        assertEquals(2, ((List<?>) detail.get("images")).size());

        ExploreTopicView updated = topicAppService.update(ExploreTopicCmd.builder()
                .topicId(published.getTopic().getTopicId())
                .name("图集专题")
                .summary("图集摘要")
                .images(List.of(testImage("https://assets.example/t3.png", "image/png")))
                .articleRevisionIds(List.of(article.getRevision().getRevisionId()))
                .lockVersion(published.getTopic().getLockVersion())
                .operator(OPERATOR)
                .build());
        assertEquals(1, updated.getRevision().getImages().size());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_topic_image WHERE topic_revision_id = ?",
                Integer.class, updated.getRevision().getRevisionId()));

        ExploreException tooMany = assertThrows(ExploreException.class,
                () -> topicAppService.create(ExploreTopicCmd.builder()
                        .name("超过九图专题")
                        .images(java.util.stream.IntStream.rangeClosed(1, 10)
                                .mapToObj(index -> testImage("https://assets.example/t" + index + ".png",
                                        "image/png")).toList())
                        .operator(OPERATOR)
                        .build()));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, tooMany.getErrorCode());
    }

    @Test
    @DisplayName("AT-023：跳转模式互斥（非专题详情不得关联文章）")
    void 专题跳转互斥() {
        ExploreArticleView article = publishedArticle("互斥文章", "<p>正文</p>");
        ExploreException conflict = assertThrows(ExploreException.class,
                () -> topicAppService.create(ExploreTopicCmd.builder()
                        .name("文章详情专题")
                        .target(target(ExploreTargetType.ARTICLE, article.getArticle().getArticleId(),
                                article.getRevision().getRevisionId()))
                        .articleRevisionIds(List.of(article.getRevision().getRevisionId()))
                        .operator(OPERATOR)
                        .build()));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, conflict.getErrorCode());

        ExploreTopicView articleMode = topicAppService.create(ExploreTopicCmd.builder()
                .name("文章详情专题")
                .target(target(ExploreTargetType.ARTICLE, article.getArticle().getArticleId(),
                        article.getRevision().getRevisionId()))
                .articleRevisionIds(List.of())
                .operator(OPERATOR)
                .build());
        ExploreTopicView published = topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(articleMode.getTopic().getTopicId()).operator(OPERATOR).build());
        assertEquals(ExploreTargetType.ARTICLE, published.getRevision().getTarget().getType());

        ExploreException otherTopic = assertThrows(ExploreException.class,
                () -> topicAppService.create(ExploreTopicCmd.builder()
                        .name("其他专题目标")
                        .target(target(ExploreTargetType.TOPIC, "tp_other", "tr_other"))
                        .operator(OPERATOR)
                        .build()));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, otherTopic.getErrorCode());
    }

    @Test
    @DisplayName("AT-024：专题卡片目标与图集在页面发布时物化，旧发布版本不变")
    void 专题卡片目标发布物化() {
        ExploreArticleView article = publishedArticle("物化文章", "<p>正文</p>");
        ExploreTopicView topic = publishedTopic("物化专题", article);
        ExploreTopicView articleMode = topicAppService.update(ExploreTopicCmd.builder()
                .topicId(topic.getTopic().getTopicId())
                .name("物化专题")
                .summary("摘要")
                .images(List.of(testImage("https://assets.example/m1.png", "image/png")))
                .target(target(ExploreTargetType.ARTICLE, article.getArticle().getArticleId(),
                        article.getRevision().getRevisionId()))
                .articleRevisionIds(List.of())
                .lockVersion(topic.getTopic().getLockVersion())
                .operator(OPERATOR)
                .build());
        ExploreTopicView articleModePublished = topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(articleMode.getTopic().getTopicId()).operator(OPERATOR).build());

        savePage(0, List.of(topicComponent(articleModePublished)));
        ExplorePublishResult firstRelease = publishPage("req-cr004-1", null, "IMMEDIATE", null, null);
        assertEquals("ARTICLE", jdbcTemplate.queryForObject(
                "SELECT i.target_type FROM cms_explore_page_item i"
                        + " JOIN cms_explore_page_component c ON c.component_id = i.component_id"
                        + " WHERE c.page_id = ? AND i.item_type = 'TOPIC'",
                String.class, firstRelease.getPage().getPageId()));

        ExploreTopicView backToDetail = topicAppService.update(ExploreTopicCmd.builder()
                .topicId(articleModePublished.getTopic().getTopicId())
                .name("物化专题")
                .summary("摘要")
                .images(List.of(testImage("https://assets.example/m1.png", "image/png")))
                .articleRevisionIds(List.of(article.getRevision().getRevisionId()))
                .lockVersion(articleModePublished.getTopic().getLockVersion())
                .operator(OPERATOR)
                .build());
        ExploreTopicView detailPublished = topicAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(backToDetail.getTopic().getTopicId()).operator(OPERATOR).build());

        ExplorePageData draft = savePage(1, List.of(topicComponent(detailPublished)));
        assertEquals("TOPIC", jdbcTemplate.queryForObject(
                "SELECT i.target_type FROM cms_explore_page_item i"
                        + " JOIN cms_explore_page_component c ON c.component_id = i.component_id"
                        + " WHERE c.page_id = ?",
                String.class, draft.getPage().getPageId()));
        ExplorePublishResult secondRelease = publishPage("req-cr004-2",
                firstRelease.getPage().getPageId(), "IMMEDIATE", null, null);
        assertEquals("TOPIC", jdbcTemplate.queryForObject(
                "SELECT i.target_type FROM cms_explore_page_item i"
                        + " JOIN cms_explore_page_component c ON c.component_id = i.component_id"
                        + " WHERE c.page_id = ?",
                String.class, secondRelease.getPage().getPageId()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_page_item i"
                        + " JOIN cms_explore_page_component c ON c.component_id = i.component_id"
                        + " WHERE c.page_id = ? AND i.target_type = 'ARTICLE'",
                Integer.class, firstRelease.getPage().getPageId()));
    }

    @Test
    @DisplayName("AT-025：V1.1.4 封面回填防重与异常 TOPIC 目标清理")
    void 专题图集迁移兼容() {
        ExploreArticleView article = publishedArticle("迁移文章", "<p>正文</p>");
        ExploreTopicView topic = publishedTopic("迁移专题", article);
        String revisionId = topic.getRevision().getRevisionId();
        String backfill = "INSERT INTO cms_explore_topic_image"
                + " (topic_revision_id, sort_no, url, width, height, mime, size)"
                + " SELECT revision_id, 10, cover_url, cover_width, cover_height, cover_mime, cover_size"
                + " FROM cms_explore_topic_revision"
                + " WHERE cover_url IS NOT NULL AND cover_url <> ''"
                + " AND cover_width IS NOT NULL AND cover_height IS NOT NULL"
                + " AND cover_mime IS NOT NULL AND cover_size IS NOT NULL"
                + " AND NOT EXISTS (SELECT 1 FROM cms_explore_topic_image i"
                + " WHERE i.topic_revision_id = cms_explore_topic_revision.revision_id)";
        jdbcTemplate.update("UPDATE cms_explore_topic_revision SET cover_url = ?,"
                        + " cover_width = 1200, cover_height = 800, cover_mime = 'image/png', cover_size = 1024"
                        + " WHERE revision_id = ?",
                "https://assets.example/legacy-topic.png", revisionId);

        jdbcTemplate.update(backfill);
        jdbcTemplate.update(backfill);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_topic_image WHERE topic_revision_id = ?",
                Integer.class, revisionId));

        jdbcTemplate.update("UPDATE cms_explore_topic_revision SET target_type = 'TOPIC',"
                + " target_ref_id = 'tp_other', target_revision_id = 'tr_other' WHERE revision_id = ?", revisionId);
        jdbcTemplate.update("UPDATE cms_explore_topic_revision SET target_type = NULL,"
                + " target_ref_id = NULL, target_revision_id = NULL"
                + " WHERE target_type = 'TOPIC' AND target_ref_id IS NOT NULL"
                + " AND target_ref_id <> topic_id");
        assertNull(jdbcTemplate.queryForObject(
                "SELECT target_type FROM cms_explore_topic_revision WHERE revision_id = ?",
                String.class, revisionId));
    }

    private ExploreBannerView publishedBanner(ExploreArticleView article) {
        ExploreBannerView created = bannerAppService.create(ExploreBannerCmd.builder()
                .title("Banner")
                .image(ExploreImage.builder()
                        .url("https://assets.example/banner.webp")
                        .width(1600)
                        .height(800)
                        .mime("image/webp")
                        .size(2048L)
                        .build())
                .target(ExploreTarget.builder()
                        .type(ExploreTargetType.ARTICLE)
                        .refId(article.getArticle().getArticleId())
                        .revisionId(article.getRevision().getRevisionId())
                        .build())
                .defaultSortNo(10)
                .operator(OPERATOR)
                .build());
        return bannerAppService.publish(ExploreContentPublishCmd.builder()
                .objectId(created.getBanner().getBannerId())
                .operator(OPERATOR)
                .build());
    }

    private ExplorePageData savePage(int lockVersion, List<ExplorePageSaveCmd.Component> components) {
        return pageAppService.save(ExplorePageSaveCmd.builder()
                .channelCode(CHANNEL)
                .lockVersion(lockVersion)
                .components(components)
                .operator(OPERATOR)
                .build());
    }

    private ExplorePublishResult publishPage(String requestId, String expectedReleaseId,
                                              String publishType, Instant effectiveAt, Instant expireAt) {
        return publishAppService.publish(ExplorePagePublishCmd.builder()
                .channelCode(CHANNEL)
                .requestId(requestId)
                .expectedReleaseId(expectedReleaseId)
                .publishType(publishType)
                .effectiveAt(effectiveAt)
                .expireAt(expireAt)
                .operator(OPERATOR)
                .build());
    }

    private ExplorePageSaveCmd.Component bannerComponent(ExploreBannerView banner) {
        return ExplorePageSaveCmd.Component.builder()
                .type(ExploreComponentType.BANNER_CAROUSEL.name())
                .sortNo(10)
                .items(List.of(item(ExploreItemType.BANNER, banner.getBanner().getBannerId(),
                        banner.getRevision().getRevisionId(), 10)))
                .build();
    }

    private ExplorePageSaveCmd.Component topicComponent(ExploreTopicView topic) {
        return ExplorePageSaveCmd.Component.builder()
                .type(ExploreComponentType.TOPIC_GRID.name())
                .sortNo(20)
                .items(List.of(item(ExploreItemType.TOPIC, topic.getTopic().getTopicId(),
                        topic.getRevision().getRevisionId(), 10)))
                .build();
    }

    private ExplorePageSaveCmd.Component feedComponent(List<ExploreArticleView> articles) {
        List<ExplorePageSaveCmd.Item> items = new ArrayList<>();
        int sortNo = 10;
        for (ExploreArticleView article : articles) {
            items.add(item(ExploreItemType.ARTICLE, article.getArticle().getArticleId(),
                    article.getRevision().getRevisionId(), sortNo));
            sortNo += 10;
        }
        return ExplorePageSaveCmd.Component.builder()
                .type(ExploreComponentType.ARTICLE_FEED.name())
                .sortNo(30)
                .items(items)
                .build();
    }

    private ExplorePageSaveCmd.Item item(ExploreItemType type, String refId, String revisionId, int sortNo) {
        return ExplorePageSaveCmd.Item.builder()
                .itemType(type.name())
                .refId(refId)
                .refRevisionId(revisionId)
                .sortNo(sortNo)
                .build();
    }

    private int countIndex(String table, String index) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics"
                        + " WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class, table, index);
        return count == null ? 0 : count;
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusLanguageDriverAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration.class
    })
    @ComponentScan(basePackages = "net.hwyz.iov.cloud.mkt.cms.service",
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CmsApplication.class))
    @Import({MyBatisPlusConfig.class, MyBatisMetaObjectHandler.class})
    @MapperScan("net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper")
    static class TestApplication {
    }
}
