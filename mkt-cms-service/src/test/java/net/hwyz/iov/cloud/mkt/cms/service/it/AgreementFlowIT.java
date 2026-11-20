package net.hwyz.iov.cloud.mkt.cms.service.it;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration;
import net.hwyz.iov.cloud.framework.mysql.config.MyBatisMetaObjectHandler;
import net.hwyz.iov.cloud.framework.mysql.config.MyBatisPlusConfig;
import net.hwyz.iov.cloud.mkt.cms.service.CmsApplication;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.CreateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.FreezeAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.PublishAgreementBundleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.UpdateAgreementDraftCmd;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementScopeQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleContentResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementBundleResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementDraftResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementRevisionResult;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementDraftAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementPublishAppService;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议子域真实 MySQL 集成测试（MKT-CMS-DSN-CR-001 §9 AT-001～AT-007）。
 * <p>
 * 执行方式（需要本地或 CI MySQL，并有可用库，默认 {@code db_cms}）：
 * <pre>
 * mvn test -Dit.mysql.password=&lt;password&gt; [-Dit.mysql.username=root] \
 *          [-Dit.mysql.host=127.0.0.1] [-Dit.mysql.db=db_cms]
 * </pre>
 * 未提供密码时显式跳过（不伪造通过）。本测试使用真实事务与真实并发，
 * 覆盖唯一键、乐观锁、发布回滚、幂等与并发切换指针。
 *
 * @author hwyz_leo
 */
@EnabledIfSystemProperty(named = "it.mysql.password", matches = ".+")
@SpringBootTest(classes = AgreementFlowIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.datasource.url=jdbc:mysql://${it.mysql.host:127.0.0.1}:3306/${it.mysql.db:db_cms}"
                        + "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
                        + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
                        + "&forceConnectionTimeZoneToSession=true&preserveInstants=true",
                "spring.datasource.username=${it.mysql.username:root}",
                "spring.datasource.password=${it.mysql.password:}",
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=0",
                "spring.flyway.table=flyway_schema_history",
                "spring.flyway.validate-on-migrate=true",
                "iov.web.jackson.zone-id=UTC",
                "iov.mysql.mapper-validation.mode=fail",
                "mybatis-plus.mapper-locations=classpath*:mappers/*.xml",
                // 隔离夹具：启用第二市场/语言/品牌/车系，验证范围隔离与非法归属拒绝（不代表首版开放）
                "cms.agreement.supported.markets=DEFAULT,CN",
                "cms.agreement.supported.locales=zh-CN,en-US",
                "cms.agreement.supported.brands=DEFAULT,BM",
                "cms.agreement.supported.car-lines=DEFAULT,CL1",
                "cms.agreement.supported.brand-car-lines=BM:CL1"
        })
class AgreementFlowIT {

    private static final LocalDate AGREEMENT_DATE = LocalDate.of(2026, 11, 20);

    @Autowired
    private AgreementDraftAppService agreementDraftAppService;
    @Autowired
    private AgreementPublishAppService agreementPublishAppService;
    @Autowired
    private AgreementContentAppService agreementContentAppService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM cms_agreement_current");
        jdbcTemplate.update("DELETE FROM cms_agreement_bundle");
        jdbcTemplate.update("DELETE FROM cms_agreement_revision");
        jdbcTemplate.update("DELETE FROM cms_agreement_draft");
        jdbcTemplate.update("DELETE FROM cms_agreement_operation");
    }

    @Test
    @DisplayName("Flyway V1.0.0 建立五张协议表与关键唯一键/外键")
    void 迁移可执行() throws Exception {
        for (String table : new String[]{"cms_agreement_draft", "cms_agreement_revision",
                "cms_agreement_bundle", "cms_agreement_current", "cms_agreement_operation"}) {
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
        assertEquals(1, countIndex("cms_agreement_revision", "uk_identity_version"));
        assertEquals(1, countIndex("cms_agreement_revision", "uk_source_draft"));
        assertEquals(1, countIndex("cms_agreement_bundle", "uk_request_id"));
    }

    @Test
    @DisplayName("草稿：保存清洗后正文、乐观锁递增、旧版本修改被拒绝")
    void 草稿与乐观锁() {
        AgreementDraftResult created = agreementDraftAppService.create(createCmd("PRIVACY_POLICY",
                "<p>正文</p><script>alert(1)</script>", null, null));
        assertEquals("<p>正文</p>", created.getBody());
        assertTrue(created.isAdjusted());
        assertEquals(0, created.getLockVersion());

        AgreementDraftResult updated = agreementDraftAppService.update(UpdateAgreementDraftCmd.builder()
                .draftId(created.getDraftId())
                .lockVersion(0)
                .body("<h2>新标题</h2><p>新正文</p>")
                .operator("tester")
                .build());
        assertEquals(1, updated.getLockVersion());
        assertEquals("<h2>新标题</h2><p>新正文</p>", updated.getBody());

        AgreementException conflict = assertThrows(AgreementException.class,
                () -> agreementDraftAppService.update(UpdateAgreementDraftCmd.builder()
                        .draftId(created.getDraftId())
                        .lockVersion(0)
                        .body("<p>过期覆盖</p>")
                        .operator("tester")
                        .build()));
        assertEquals(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT, conflict.getErrorCode());
        assertEquals("<h2>新标题</h2><p>新正文</p>",
                agreementDraftAppService.get(created.getDraftId()).getBody());
    }

    @Test
    @DisplayName("定稿：同草稿版本重复定稿返回同一修订，修订正文与摘要不变")
    void 定稿幂等() {
        AgreementDraftResult draft = agreementDraftAppService.create(createCmd("PRIVACY_POLICY",
                "<p>隐私政策</p>", null, null));

        AgreementRevisionResult first = freeze(draft.getDraftId(), draft.getLockVersion());
        AgreementRevisionResult second = freeze(draft.getDraftId(), draft.getLockVersion());

        assertEquals(first.getRevisionId(), second.getRevisionId());
        assertEquals(first.getContentHash(), second.getContentHash());
        assertEquals("<p>隐私政策</p>", first.getBody());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_revision", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_operation WHERE action = 'FREEZE'", Integer.class));
    }

    @Test
    @DisplayName("定稿：同身份同业务版本换成另一正文被唯一键拒绝")
    void 定稿版本唯一() {
        AgreementDraftResult firstDraft = agreementDraftAppService.create(createCmd("PRIVACY_POLICY",
                "<p>正文一</p>", null, null));
        freeze(firstDraft.getDraftId(), firstDraft.getLockVersion());

        AgreementDraftResult secondDraft = agreementDraftAppService.create(createCmd("PRIVACY_POLICY",
                "<p>正文二</p>", null, null));

        AgreementException conflict = assertThrows(AgreementException.class,
                () -> freeze(secondDraft.getDraftId(), secondDraft.getLockVersion()));
        assertEquals(AgreementErrorCode.AGREEMENT_VERSION_CONFLICT, conflict.getErrorCode());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_revision", Integer.class));
    }

    @Test
    @DisplayName("发布：成套发布后可一次读取两份正文，历史修订仍可精确回读")
    void 发布与读取() {
        AgreementRevisionResult privacy = freezeDraft("PRIVACY_POLICY", "<p>隐私政策正文</p>", null, null);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议正文</p>", null, null);

        AgreementBundleResult published = publish("req-1", privacy.getRevisionId(), user.getRevisionId(), null);
        assertNotNull(published.getBundleId());

        AgreementBundleContentResult current = agreementContentAppService.getCurrentBundle(AgreementScopeQuery.builder()
                .scene("IVI_ACTIVATION").build());
        assertEquals(published.getBundleId(), current.getBundleId());
        assertEquals("DEFAULT", current.getMarket());
        assertEquals("zh-CN", current.getLocale());
        assertEquals(2, current.getAgreements().size());
        assertEquals("<p>隐私政策正文</p>", current.getAgreements().get(0).getBody());
        assertEquals("<p>用户协议正文</p>", current.getAgreements().get(1).getBody());

        AgreementBundleContentResult.AgreementContentResult history =
                agreementContentAppService.getPublishedRevision(privacy.getRevisionId());
        assertEquals(privacy.getContentHash(), history.getContentHash());
        assertEquals("<p>隐私政策正文</p>", history.getBody());

        // 切换新包后旧修订仍可回读，新正文不再指向旧内容
        AgreementRevisionResult privacyV2 = freezeDraft("PRIVACY_POLICY", "<p>隐私政策正文v2</p>", null, null, "2.0");
        AgreementBundleResult publishedV2 = publish("req-2", privacyV2.getRevisionId(), user.getRevisionId(),
                published.getBundleId());
        assertTrue(!publishedV2.getBundleId().equals(published.getBundleId()));
        assertEquals("<p>隐私政策正文v2</p>", agreementContentAppService
                .getCurrentBundle(AgreementScopeQuery.builder().scene("IVI_ACTIVATION").build())
                .getAgreements().get(0).getBody());
        assertEquals(privacy.getContentHash(), agreementContentAppService
                .getPublishedRevision(privacy.getRevisionId()).getContentHash());
    }

    @Test
    @DisplayName("发布：缺项/未发布修订/跨范围均拒绝且当前指针不变")
    void 发布失败不改变指针() {
        AgreementRevisionResult privacy = freezeDraft("PRIVACY_POLICY", "<p>隐私政策</p>", null, null);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议</p>", null, null);
        AgreementBundleResult first = publish("req-1", privacy.getRevisionId(), user.getRevisionId(), null);

        AgreementException missingRevision = assertThrows(AgreementException.class,
                () -> publish("req-2", privacy.getRevisionId(), "REV-NOT-EXIST", first.getBundleId()));
        assertEquals(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND, missingRevision.getErrorCode());

        AgreementRevisionResult privacyCn = freezeDraft("PRIVACY_POLICY", "<p>CN 隐私</p>", "CN", null);
        AgreementException crossScope = assertThrows(AgreementException.class,
                () -> publish("req-3", privacyCn.getRevisionId(), user.getRevisionId(), first.getBundleId()));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, crossScope.getErrorCode());

        AgreementException wrongType = assertThrows(AgreementException.class,
                () -> publish("req-4", user.getRevisionId(), privacy.getRevisionId(), first.getBundleId()));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, wrongType.getErrorCode());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_bundle", Integer.class));
        assertEquals(first.getBundleId(), agreementContentAppService.getCurrentBundle(
                AgreementScopeQuery.builder().scene("IVI_ACTIVATION").build()).getBundleId());
    }

    @Test
    @DisplayName("发布幂等：同 requestId 同命令只生成一个包；不同命令拒绝")
    void 发布幂等与冲突() {
        AgreementRevisionResult privacy = freezeDraft("PRIVACY_POLICY", "<p>隐私政策</p>", null, null);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议</p>", null, null);

        AgreementBundleResult first = publish("req-1", privacy.getRevisionId(), user.getRevisionId(), null);
        AgreementBundleResult replay = publish("req-1", privacy.getRevisionId(), user.getRevisionId(), null);
        assertEquals(first.getBundleId(), replay.getBundleId());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_bundle", Integer.class));

        AgreementException idempotencyConflict = assertThrows(AgreementException.class,
                () -> publish("req-1", privacy.getRevisionId(), user.getRevisionId(), first.getBundleId()));
        assertEquals(AgreementErrorCode.AGREEMENT_IDEMPOTENCY_CONFLICT, idempotencyConflict.getErrorCode());
    }

    @Test
    @DisplayName("发布前提：过期 expectedBundleId 拒绝覆盖")
    void 发布前提失效() {
        AgreementRevisionResult privacy = freezeDraft("PRIVACY_POLICY", "<p>隐私政策</p>", null, null);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议</p>", null, null);
        AgreementBundleResult first = publish("req-1", privacy.getRevisionId(), user.getRevisionId(), null);

        AgreementException stale = assertThrows(AgreementException.class,
                () -> publish("req-stale", privacy.getRevisionId(), user.getRevisionId(), null));
        assertEquals(AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT, stale.getErrorCode());

        AgreementException staleId = assertThrows(AgreementException.class,
                () -> publish("req-stale-2", privacy.getRevisionId(), user.getRevisionId(), "BDL-NOT-EXIST"));
        assertEquals(AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT, staleId.getErrorCode());
        assertEquals(first.getBundleId(), agreementContentAppService.getCurrentBundle(
                AgreementScopeQuery.builder().scene("IVI_ACTIVATION").build()).getBundleId());
    }

    @Test
    @DisplayName("并发：同 requestId 首发布只生成一个包，两个请求得到同一结果")
    void 并发同请求幂等() throws Exception {
        AgreementRevisionResult privacy = freezeDraft("PRIVACY_POLICY", "<p>隐私政策</p>", null, null);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议</p>", null, null);

        List<PublishOutcome> outcomes = runConcurrently(2, index -> publish("req-concurrent-1",
                privacy.getRevisionId(), user.getRevisionId(), null));

        long success = outcomes.stream().filter(PublishOutcome::success).count();
        assertEquals(2, success, "同 requestId 并发必须都拿到同一结果: " + outcomes);
        assertEquals(outcomes.get(0).bundleId, outcomes.get(1).bundleId);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_bundle", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_operation WHERE action = 'PUBLISH'", Integer.class));
    }

    @Test
    @DisplayName("并发：两个不同 requestId 同旧前提发布，最多一个成功")
    void 并发不同请求最多一个成功() throws Exception {
        AgreementRevisionResult privacy = freezeDraft("PRIVACY_POLICY", "<p>隐私政策</p>", null, null);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议</p>", null, null);

        List<PublishOutcome> outcomes = runConcurrently(2, index -> publish("req-race-" + index,
                privacy.getRevisionId(), user.getRevisionId(), null));

        assertEquals(1, outcomes.stream().filter(PublishOutcome::success).count(),
                "同旧前提并发发布必须只有一个成功: " + outcomes);
        assertTrue(outcomes.stream().anyMatch(outcome ->
                        AgreementErrorCode.AGREEMENT_PUBLISH_CONFLICT == outcome.errorCode
                                || AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE == outcome.errorCode),
                "失败方必须是发布冲突或暂不可用: " + outcomes);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_bundle", Integer.class));
    }

    @Test
    @DisplayName("范围隔离：另一市场无内容明确失败，不回退、不跨市场混用")
    void 范围隔离与无回退() {
        AgreementRevisionResult privacy = freezeDraft("PRIVACY_POLICY", "<p>隐私政策</p>", null, null);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议</p>", null, null);
        publish("req-1", privacy.getRevisionId(), user.getRevisionId(), null);

        AgreementException notFound = assertThrows(AgreementException.class,
                () -> agreementContentAppService.getCurrentBundle(AgreementScopeQuery.builder()
                        .scene("IVI_ACTIVATION").market("CN").build()));
        assertEquals(AgreementErrorCode.AGREEMENT_CURRENT_NOT_FOUND, notFound.getErrorCode());

        AgreementException unsupported = assertThrows(AgreementException.class,
                () -> agreementContentAppService.getCurrentBundle(AgreementScopeQuery.builder()
                        .scene("IVI_ACTIVATION").brandCode("DEFAULT").carLineCode("CL1").build()));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, unsupported.getErrorCode());
    }

    @Test
    @DisplayName("权限/审计：成功维护、定稿与发布均留痕，操作人来自调用上下文")
    void 操作留痕() {
        AgreementDraftResult draft = agreementDraftAppService.create(createCmd("PRIVACY_POLICY",
                "<p>隐私政策</p>", null, null));
        agreementDraftAppService.update(UpdateAgreementDraftCmd.builder()
                .draftId(draft.getDraftId()).lockVersion(0).body("<p>隐私政策 v2</p>")
                .operator("editor").build());
        AgreementRevisionResult privacy = freeze(draft.getDraftId(), 1);
        AgreementRevisionResult user = freezeDraft("USER_AGREEMENT", "<p>用户协议</p>", null, null);
        publish("req-1", privacy.getRevisionId(), user.getRevisionId(), null);

        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_operation WHERE action = 'CREATE_DRAFT'", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_operation WHERE action = 'UPDATE_DRAFT' "
                        + "AND operator = 'editor'", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_operation WHERE action = 'FREEZE'", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_agreement_operation WHERE action = 'PUBLISH' "
                        + "AND result = 'SUCCESS' AND object_id IS NOT NULL", Integer.class));
    }

    // ==================== 辅助方法 ====================

    private CreateAgreementDraftCmd createCmd(String type, String body, String market, String locale) {
        return createCmd(type, body, market, locale, "1.0");
    }

    private CreateAgreementDraftCmd createCmd(String type, String body, String market, String locale,
                                              String version) {
        return CreateAgreementDraftCmd.builder()
                .agreementType(type)
                .scene("IVI_ACTIVATION")
                .market(market)
                .locale(locale)
                .title(type + " 标题")
                .version(version)
                .agreementDate(AGREEMENT_DATE)
                .body(body)
                .operator("tester")
                .build();
    }

    private AgreementRevisionResult freeze(String draftId, int lockVersion) {
        return agreementDraftAppService.freeze(FreezeAgreementDraftCmd.builder()
                .draftId(draftId)
                .lockVersion(lockVersion)
                .operator("tester")
                .build());
    }

    private AgreementRevisionResult freezeDraft(String type, String body, String market, String locale) {
        return freezeDraft(type, body, market, locale, "1.0");
    }

    private AgreementRevisionResult freezeDraft(String type, String body, String market, String locale,
                                                String version) {
        AgreementDraftResult draft = agreementDraftAppService.create(
                createCmd(type, body, market, locale, version));
        return freeze(draft.getDraftId(), draft.getLockVersion());
    }

    private AgreementBundleResult publish(String requestId, String privacyRevisionId, String userRevisionId,
                                          String expectedBundleId) {
        return agreementPublishAppService.publish(PublishAgreementBundleCmd.builder()
                .scene("IVI_ACTIVATION")
                .privacyRevisionId(privacyRevisionId)
                .userAgreementRevisionId(userRevisionId)
                .requestId(requestId)
                .expectedBundleId(expectedBundleId)
                .expectedBundleIdPresent(true)
                .operator("publisher")
                .build());
    }

    private int countIndex(String table, String indexName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                             + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private List<PublishOutcome> runConcurrently(int threads, PublishCall call) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger index = new AtomicInteger();
        List<Future<PublishOutcome>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                int current = index.getAndIncrement();
                Callable<PublishOutcome> task = () -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    try {
                        return PublishOutcome.success(call.publish(current).getBundleId());
                    } catch (AgreementException e) {
                        return PublishOutcome.failure((AgreementErrorCode) e.getErrorCode());
                    }
                };
                futures.add(executor.submit(task));
            }
            List<PublishOutcome> outcomes = new ArrayList<>();
            for (Future<PublishOutcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface PublishCall {
        AgreementBundleResult publish(int index);
    }

    private static final class PublishOutcome {

        private final boolean success;
        private final String bundleId;
        private final AgreementErrorCode errorCode;

        private PublishOutcome(boolean success, String bundleId, AgreementErrorCode errorCode) {
            this.success = success;
            this.bundleId = bundleId;
            this.errorCode = errorCode;
        }

        private static PublishOutcome success(String bundleId) {
            return new PublishOutcome(true, bundleId, null);
        }

        private static PublishOutcome failure(AgreementErrorCode errorCode) {
            return new PublishOutcome(false, null, errorCode);
        }

        private boolean success() {
            return success;
        }

        @Override
        public String toString() {
            return success ? "SUCCESS(" + bundleId + ")" : "FAIL(" + errorCode + ")";
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusLanguageDriverAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @ComponentScan(basePackages = "net.hwyz.iov.cloud.mkt.cms.service",
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CmsApplication.class))
    @Import({MyBatisPlusConfig.class, MyBatisMetaObjectHandler.class})
    @MapperScan("net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper")
    static class TestApplication {
    }
}
