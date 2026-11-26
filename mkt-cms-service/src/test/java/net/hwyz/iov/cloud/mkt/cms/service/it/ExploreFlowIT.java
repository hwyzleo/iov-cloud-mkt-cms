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
                "cms_explore_page_current", "cms_explore_operation"}) {
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
                page.getReleaseId(), feed.getNextCursor(), 2);
        assertEquals(1, ((List<?>) next.get("items")).size());
        assertEquals(false, next.get("hasMore"));

        Map<String, Object> articleDetail = mobileQueryAppService.articleDetail(
                articles.get(0).getArticle().getArticleId(),
                articles.get(0).getRevision().getRevisionId());
        assertEquals("<p>正文1</p>", articleDetail.get("body"));

        Map<String, Object> topicDetail = mobileQueryAppService.topicDetail(
                topic.getTopic().getTopicId(), topic.getRevision().getRevisionId(), null, 10);
        assertEquals(1, ((List<?>) topicDetail.get("items")).size());
        assertEquals("专题一", topicDetail.get("name"));

        ExploreException changed = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.articleFeed(CHANNEL, "er_missing", null, 10));
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
    @DisplayName("AT-014：页面草稿乐观锁冲突不静默覆盖")
    void 页面草稿乐观锁() {
        ExploreArticleView article = publishedArticle("文章", "<p>正文</p>");
        ExplorePageData first = savePage(0, List.of(feedComponent(List.of(article))));
        assertEquals(1, first.getPage().getLockVersion());
        ExploreException conflict = assertThrows(ExploreException.class,
                () -> savePage(0, List.of(feedComponent(List.of(article)))));
        assertEquals(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, conflict.getErrorCode());
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
