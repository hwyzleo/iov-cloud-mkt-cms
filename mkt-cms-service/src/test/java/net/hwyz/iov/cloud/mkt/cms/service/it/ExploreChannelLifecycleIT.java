package net.hwyz.iov.cloud.mkt.cms.service.it;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration;
import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.framework.mysql.config.MyBatisMetaObjectHandler;
import net.hwyz.iov.cloud.framework.mysql.config.MyBatisPlusConfig;
import net.hwyz.iov.cloud.mkt.cms.service.CmsApplication;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.ExploreChannelPublishCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExploreChannelView;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreChannelAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreMobileQueryAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 探索页频道独立生命周期真实 MySQL 集成测试（MKT-CMS-DSN-CR-005 / AT-026～AT-029）。
 * <p>
 * 执行方式（需要本地或 CI MySQL）：
 * <pre>
 * mvn test -Dit.mysql.password=&lt;password&gt; [-Dit.mysql.username=root] \
 *          [-Dit.mysql.host=127.0.0.1] [-Dit.mysql.db=db_cms_explore_it]
 * </pre>
 * 未提供密码时显式跳过（不伪造通过）。
 *
 * @author hwyz_leo
 */
@EnabledIfSystemProperty(named = "it.mysql.password", matches = ".+")
@SpringBootTest(classes = ExploreChannelLifecycleIT.TestApplication.class,
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
class ExploreChannelLifecycleIT {

    private static final String OPERATOR = "tester";

    private static final List<String> PRESET_CHANNELS = List.of("RECOMMEND", "ACTIVITY", "LOCAL");

    @Autowired
    private ExploreChannelAppService channelAppService;

    @Autowired
    private ExploreMobileQueryAppService mobileQueryAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ImageService imageService;

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
        jdbcTemplate.update("DELETE FROM cms_explore_channel_revision WHERE channel_code NOT IN (?,?,?)",
                PRESET_CHANNELS.toArray());
        jdbcTemplate.update("DELETE FROM cms_explore_channel WHERE channel_code NOT IN (?,?,?)",
                PRESET_CHANNELS.toArray());
        jdbcTemplate.update("DELETE FROM cms_explore_channel_revision"
                + " WHERE channel_code IN (?,?,?) AND revision_no > 1", PRESET_CHANNELS.toArray());
        jdbcTemplate.update("UPDATE cms_explore_channel_revision SET status = 'ENABLED', revision_status = 'PUBLISHED'"
                + " WHERE channel_code IN (?,?,?) AND revision_no = 1", PRESET_CHANNELS.toArray());
        jdbcTemplate.update("UPDATE cms_explore_channel c"
                + " SET c.draft_revision_id = NULL, c.status = 'ENABLED', c.lock_version = 0,"
                + " c.current_revision_id = (SELECT r.revision_id FROM cms_explore_channel_revision r"
                + "  WHERE r.channel_code = c.channel_code AND r.revision_no = 1 LIMIT 1)"
                + " WHERE c.channel_code IN (?,?,?)", PRESET_CHANNELS.toArray());
    }

    @Test
    @DisplayName("AT-029：V1.1.5 修订表/双指针/回填防重与 V1.1.6 详情可见默认 true")
    void 迁移与存量兼容() {
        assertEquals(1, countTable("cms_explore_channel_revision"));
        assertEquals(1, countColumn("cms_explore_channel", "draft_revision_id"));
        assertEquals(1, countColumn("cms_explore_channel", "current_revision_id"));
        assertEquals(1, countColumn("cms_explore_topic_image", "show_in_detail"));
        assertEquals("1", jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns WHERE table_schema = DATABASE()"
                        + " AND table_name = 'cms_explore_topic_image' AND column_name = 'show_in_detail'",
                String.class));

        for (String channelCode : PRESET_CHANNELS) {
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cms_explore_channel_revision WHERE channel_code = ?"
                            + " AND revision_no = 1 AND revision_status = 'PUBLISHED'",
                    Integer.class, channelCode), channelCode);
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cms_explore_channel c JOIN cms_explore_channel_revision r"
                            + " ON r.revision_id = c.current_revision_id"
                            + " WHERE c.channel_code = ? AND r.revision_no = 1 AND r.revision_status = 'PUBLISHED'",
                    Integer.class, channelCode), channelCode);
        }

        // 重复执行回填语句不得重复生成修订
        String backfill = """
                INSERT INTO `cms_explore_channel_revision` (
                    `revision_id`, `channel_code`, `revision_no`, `name`, `sort_no`, `status`,
                    `revision_status`, `content_hash`, `published_by`, `published_at`,
                    `create_by`, `create_time`, `modify_by`, `modify_time`
                )
                SELECT
                    CONCAT('ecr_', UPPER(SUBSTRING(SHA2(CONCAT('CHANNEL|', `c`.`channel_code`, '|1'), 256), 1, 20))),
                    `c`.`channel_code`, 1, `c`.`name`, `c`.`sort_no`, `c`.`status`,
                    'PUBLISHED',
                    SHA2(CONCAT(`c`.`channel_code`, '|', `c`.`name`, '|', `c`.`sort_no`, '|', `c`.`status`), 256),
                    'system', UTC_TIMESTAMP(3), 'system', UTC_TIMESTAMP(3), 'system', UTC_TIMESTAMP(3)
                FROM `cms_explore_channel` `c`
                WHERE NOT EXISTS (
                    SELECT 1 FROM `cms_explore_channel_revision` `r`
                    WHERE `r`.`channel_code` = `c`.`channel_code` AND `r`.`revision_no` = 1
                )
                """;
        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel_revision WHERE channel_code IN (?,?,?)",
                Integer.class, PRESET_CHANNELS.toArray());
        jdbcTemplate.update(backfill);
        jdbcTemplate.update(backfill);
        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel_revision WHERE channel_code IN (?,?,?)",
                Integer.class, PRESET_CHANNELS.toArray());
        assertEquals(before, after);

        jdbcTemplate.update("INSERT INTO cms_explore_topic_image"
                        + " (topic_revision_id, sort_no, url, width, height, mime, size)"
                        + " VALUES ('etr_default_probe', 10, 'https://assets.example/probe.png', 100, 100,"
                        + " 'image/png', 1024)");
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_topic_image WHERE topic_revision_id = 'etr_default_probe'"
                        + " AND show_in_detail = TRUE", Integer.class));
        jdbcTemplate.update("DELETE FROM cms_explore_topic_image WHERE topic_revision_id = 'etr_default_probe'");
    }

    @Test
    @DisplayName("AT-026：新增/草稿修改/乐观锁/引用保护删除/审计")
    void 频道管理与删除保护() {
        ExploreChannelView created = channelAppService.create(cmd("NEWS_T", "资讯", 5, "ENABLED", null));
        assertEquals(1, created.getDraft().getRevisionNo());
        assertNull(created.getPublished());
        assertTrue(created.isHasUnpublishedChanges());
        assertTrue(created.isDeletable());
        assertEquals(0, created.getLockVersion());

        ExploreException duplicate = assertThrows(ExploreException.class,
                () -> channelAppService.create(cmd("NEWS_T", "重复", 6, "ENABLED", null)));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, duplicate.getErrorCode());
        ExploreException invalid = assertThrows(ExploreException.class,
                () -> channelAppService.create(cmd("news-lower", "非法", 6, "ENABLED", null)));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, invalid.getErrorCode());

        String firstDraftId = created.getDraft().getRevisionId();
        ExploreChannelView updated = channelAppService.update(
                cmd("NEWS_T", "资讯二", 7, "ENABLED", 0));
        assertEquals(firstDraftId, updated.getDraft().getRevisionId(), "已有草稿应原地更新");
        assertEquals("资讯二", updated.getDraft().getName());
        assertEquals(1, updated.getLockVersion());

        ExploreException conflict = assertThrows(ExploreException.class,
                () -> channelAppService.update(cmd("NEWS_T", "过期", 8, "ENABLED", 0)));
        assertEquals(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, conflict.getErrorCode());

        insertPage("ep_ref_draft", "NEWS_T", 0, "DRAFT");
        ExploreChannelView referenced = channelAppService.view("NEWS_T");
        assertTrue(!referenced.isDeletable());
        ExploreException refusal = assertThrows(ExploreException.class,
                () -> channelAppService.delete("NEWS_T", 1, OPERATOR));
        assertEquals(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, refusal.getErrorCode());
        assertTrue(refusal.getMessage().contains("只能停用"));

        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_operation WHERE object_type = 'EXPLORE_CHANNEL'"
                        + " AND object_id = 'NEWS_T' AND action IN ('CREATE_CHANNEL','UPDATE_CHANNEL')",
                Integer.class));

        jdbcTemplate.update("DELETE FROM cms_explore_page WHERE page_id = 'ep_ref_draft'");
        channelAppService.delete("NEWS_T", 1, OPERATOR);
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel WHERE channel_code = 'NEWS_T'", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel_revision WHERE channel_code = 'NEWS_T'", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_operation WHERE action = 'DELETE_CHANNEL'"
                        + " AND object_id = 'NEWS_T'", Integer.class));
    }

    @Test
    @DisplayName("AT-027：独立发布/幂等/旧前提/手机读取/停用/不影响页面发布")
    void 独立发布与手机读取() {
        channelAppService.create(cmd("NEWS_T", "资讯", 5, "ENABLED", null));
        ExploreChannelView firstPublish = channelAppService.publish(
                publishCmd("NEWS_T", "req-1", null, 0));
        String firstRevision = firstPublish.getPublished().getRevisionId();
        assertNull(firstPublish.getDraft());
        assertEquals(1, firstPublish.getLockVersion());

        insertPage("ep_news_1", "NEWS_T", 1, "PUBLISHED");
        insertCurrent("NEWS_T", "ep_news_1");

        List<Map<String, Object>> channels = mobileQueryAppService.channels();
        Map<String, Object> news = channels.stream()
                .filter(item -> "NEWS_T".equals(item.get("code"))).findFirst().orElseThrow();
        assertEquals("资讯", news.get("name"));
        assertEquals(5, news.get("sortNo"));
        assertEquals("NEWS_T", channels.get(0).get("code"), "排序应来自已发布修订");
        assertEquals("资讯", mobileQueryAppService.currentPage("NEWS_T", 10).getChannelName());

        int pageReleases = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_page", Integer.class);

        channelAppService.update(cmd("NEWS_T", "快讯", 6, "ENABLED", 1));
        assertEquals("资讯", mobileQueryAppService.channels().stream()
                        .filter(item -> "NEWS_T".equals(item.get("code"))).findFirst().orElseThrow().get("name"),
                "草稿不得提前上线");

        channelAppService.publish(publishCmd("NEWS_T", "req-2", firstRevision, 2));
        assertEquals("快讯", mobileQueryAppService.channels().stream()
                .filter(item -> "NEWS_T".equals(item.get("code"))).findFirst().orElseThrow().get("name"));
        assertEquals(pageReleases, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM cms_explore_page", Integer.class),
                "频道发布不得创建页面版本");

        int revisionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel_revision WHERE channel_code = 'NEWS_T'", Integer.class);
        ExploreChannelView retried = channelAppService.publish(
                publishCmd("NEWS_T", "req-2", firstRevision, 1));
        assertEquals(2, retried.getPublished().getRevisionNo());
        assertEquals(revisionCount, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel_revision WHERE channel_code = 'NEWS_T'", Integer.class));

        ExploreException reused = assertThrows(ExploreException.class,
                () -> channelAppService.publish(publishCmd("NEWS_T", "req-2", null, 1)));
        assertEquals(ExploreErrorCode.EXPLORE_IDEMPOTENCY_CONFLICT, reused.getErrorCode());

        channelAppService.update(cmd("NEWS_T", "快讯", 6, "ENABLED", 3));
        ExploreException staleExpected = assertThrows(ExploreException.class,
                () -> channelAppService.publish(publishCmd("NEWS_T", "req-3", firstRevision, 4)));
        assertEquals(ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT, staleExpected.getErrorCode());
        ExploreException staleLock = assertThrows(ExploreException.class,
                () -> channelAppService.publish(publishCmd("NEWS_T", "req-4", null, 1)));
        assertEquals(ExploreErrorCode.EXPLORE_VERSION_CONFLICT, staleLock.getErrorCode());

        String currentRevision = channelAppService.view("NEWS_T").getPublished().getRevisionId();
        channelAppService.update(cmd("NEWS_T", "快讯", 6, "DISABLED", 4));
        channelAppService.publish(publishCmd("NEWS_T", "req-5", currentRevision, 5));
        assertTrue(mobileQueryAppService.channels().stream()
                .noneMatch(item -> "NEWS_T".equals(item.get("code"))));
        ExploreException hidden = assertThrows(ExploreException.class,
                () -> mobileQueryAppService.currentPage("NEWS_T", 10));
        assertEquals(ExploreErrorCode.EXPLORE_PAGE_NOT_FOUND, hidden.getErrorCode());
    }

    @Test
    @DisplayName("AT-027：根行锁并发发布仅一个成功，另一个基于旧前提失败")
    void 并发发布仅一个成功() throws Exception {
        channelAppService.create(cmd("CONC_T", "并发", 90, "ENABLED", null));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> first = publishTask("CONC_T", "req-c1", start);
            Callable<Object> second = publishTask("CONC_T", "req-c2", start);
            Future<Object> firstResult = pool.submit(first);
            Future<Object> secondResult = pool.submit(second);
            start.countDown();
            Object left = firstResult.get(20, TimeUnit.SECONDS);
            Object right = secondResult.get(20, TimeUnit.SECONDS);
            long successes = List.of(left, right).stream().filter("OK"::equals).count();
            assertEquals(1, successes, "并发发布只允许一个成功: " + left + " / " + right);
            Object loser = "OK".equals(left) ? right : left;
            assertTrue(loser == ExploreErrorCode.EXPLORE_VERSION_CONFLICT
                            || loser == ExploreErrorCode.EXPLORE_PUBLISH_CONFLICT,
                    "失败方必须基于旧前提被拒绝: " + loser);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_explore_channel_revision"
                        + " WHERE channel_code = 'CONC_T' AND revision_status = 'PUBLISHED'", Integer.class));
    }

    private Callable<Object> publishTask(String channelCode, String requestId, CountDownLatch start) {
        return () -> {
            start.await();
            try {
                channelAppService.publish(publishCmd(channelCode, requestId, null, 0));
                return "OK";
            } catch (ExploreException e) {
                return e.getErrorCode();
            }
        };
    }

    private ExploreChannelCmd cmd(String channelCode, String name, int sortNo, String status,
                                  Integer lockVersion) {
        return ExploreChannelCmd.builder()
                .channelCode(channelCode)
                .name(name)
                .sortNo(sortNo)
                .status(status)
                .lockVersion(lockVersion)
                .operator(OPERATOR)
                .build();
    }

    private ExploreChannelPublishCmd publishCmd(String channelCode, String requestId,
                                                String expected, int lockVersion) {
        return ExploreChannelPublishCmd.builder()
                .channelCode(channelCode)
                .requestId(requestId)
                .expectedCurrentRevisionId(expected)
                .lockVersion(lockVersion)
                .operator(OPERATOR)
                .build();
    }

    private void insertPage(String pageId, String channelCode, int pageVersion, String status) {
        jdbcTemplate.update("INSERT INTO cms_explore_page"
                        + " (page_id, channel_code, page_version, status, lock_version,"
                        + "  create_by, create_time, modify_by, modify_time)"
                        + " VALUES (?, ?, ?, ?, 0, 'tester', UTC_TIMESTAMP(3), 'tester', UTC_TIMESTAMP(3))",
                pageId, channelCode, pageVersion, status);
    }

    private void insertCurrent(String channelCode, String releaseId) {
        jdbcTemplate.update("INSERT INTO cms_explore_page_current"
                        + " (channel_code, release_id, lock_version, updated_at)"
                        + " VALUES (?, ?, 0, UTC_TIMESTAMP(3))"
                        + " ON DUPLICATE KEY UPDATE release_id = VALUES(release_id)",
                channelCode, releaseId);
    }

    private int countTable(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"
                        + " AND table_name = ?", Integer.class, table);
        return count == null ? 0 : count;
    }

    private int countColumn(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                        + " AND table_name = ? AND column_name = ?", Integer.class, table, column);
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
            PageHelperAutoConfiguration.class
    })
    @ComponentScan(basePackages = "net.hwyz.iov.cloud.mkt.cms.service",
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CmsApplication.class))
    @Import({MyBatisPlusConfig.class, MyBatisMetaObjectHandler.class})
    @MapperScan("net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper")
    static class TestApplication {
    }
}
