package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd.PublishAgreementBundleCmd;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import net.hwyz.iov.cloud.mkt.cms.service.domain.repository.AgreementBundleRepository;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.PublishRequestHash;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ScopeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 协议发布应用服务单元测试（MKT-CMS-DSN-CR-001 §5 / AT-003 / AT-004）。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("协议发布 - 命令校验/幂等/并发竞争")
class AgreementPublishAppServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T02:00:00Z");
    private static final AgreementScope DEFAULT_SCOPE = AgreementScope.of(AgreementScene.IVI_ACTIVATION,
            "DEFAULT", "zh-CN", "DEFAULT", "DEFAULT");

    @Mock
    private AgreementBundleRepository agreementBundleRepository;
    @Mock
    private AgreementTxService agreementTxService;

    private AgreementPublishAppService service;

    @BeforeEach
    void setUp() {
        service = new AgreementPublishAppService(new ScopeResolver(new DefaultOnlyScopeProvider()),
                agreementBundleRepository, agreementTxService);
        lenient().when(agreementTxService.publish(any(), any(), any()))
                .thenReturn(bundle(PublishRequestHash.of(DEFAULT_SCOPE, "REV-P", "REV-U", null)));
    }

    @Test
    @DisplayName("expectedBundleId 字段必须出现（显式 null 表示首次发布）")
    void 前提字段必须出现() {
        PublishAgreementBundleCmd missing = cmd();
        missing.setExpectedBundleIdPresent(false);
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.publish(missing));

        PublishAgreementBundleCmd explicitNull = cmd();
        assertEquals("BDL-EXISTING", service.publish(explicitNull).getBundleId());
    }

    @Test
    @DisplayName("requestId 与修订标识校验")
    void 命令基础校验() {
        PublishAgreementBundleCmd noRequestId = cmd();
        noRequestId.setRequestId(" ");
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.publish(noRequestId));

        PublishAgreementBundleCmd tooLong = cmd();
        tooLong.setRequestId("r".repeat(65));
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.publish(tooLong));

        PublishAgreementBundleCmd sameRevision = cmd();
        sameRevision.setUserAgreementRevisionId(sameRevision.getPrivacyRevisionId());
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.publish(sameRevision));

        PublishAgreementBundleCmd blankExpected = cmd();
        blankExpected.setExpectedBundleId("  ");
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.publish(blankExpected));

        PublishAgreementBundleCmd noOperator = cmd();
        noOperator.setOperator(null);
        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.publish(noOperator));
    }

    @Test
    @DisplayName("范围非法拒绝，不进入发布事务")
    void 范围非法() {
        PublishAgreementBundleCmd cmd = cmd();
        cmd.setLocale("en-US");

        assertCode(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, () -> service.publish(cmd));
        verify(agreementTxService, times(0)).publish(any(), any(), any());
    }

    @Test
    @DisplayName("发布成功：事务使用规范化范围与命令哈希")
    void 发布成功() {
        assertEquals("BDL-EXISTING", service.publish(cmd()).getBundleId());

        ArgumentCaptor<AgreementScope> scopeCaptor = ArgumentCaptor.forClass(AgreementScope.class);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(agreementTxService).publish(scopeCaptor.capture(), hashCaptor.capture(), any());
        assertEquals(DEFAULT_SCOPE, scopeCaptor.getValue());
        assertEquals(PublishRequestHash.of(DEFAULT_SCOPE, "REV-P", "REV-U", null), hashCaptor.getValue());
    }

    @Test
    @DisplayName("同 requestId 同命令：并发唯一键竞争后返回既有发布包")
    void 同命令幂等() {
        when(agreementTxService.publish(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_request_id"));
        when(agreementBundleRepository.findByRequestId("req-1"))
                .thenReturn(Optional.of(bundle(PublishRequestHash.of(DEFAULT_SCOPE, "REV-P", "REV-U", null))));

        assertEquals("BDL-EXISTING", service.publish(cmd()).getBundleId());
    }

    @Test
    @DisplayName("同 requestId 不同命令：幂等冲突")
    void 不同命令幂等冲突() {
        when(agreementTxService.publish(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_request_id"));
        when(agreementBundleRepository.findByRequestId("req-1"))
                .thenReturn(Optional.of(bundle("different-hash")));

        assertCode(AgreementErrorCode.AGREEMENT_IDEMPOTENCY_CONFLICT, () -> service.publish(cmd()));
    }

    @Test
    @DisplayName("锁竞争重试后成功，不静默覆盖")
    void 锁竞争重试() {
        when(agreementTxService.publish(any(), any(), any()))
                .thenThrow(new PessimisticLockingFailureException("deadlock"))
                .thenReturn(bundle(PublishRequestHash.of(DEFAULT_SCOPE, "REV-P", "REV-U", null)));
        when(agreementBundleRepository.findByRequestId("req-1")).thenReturn(Optional.empty());

        assertEquals("BDL-EXISTING", service.publish(cmd()).getBundleId());
        verify(agreementTxService, times(2)).publish(any(), any(), any());
    }

    @Test
    @DisplayName("持续并发竞争：重试耗尽返回暂不可用")
    void 重试耗尽() {
        when(agreementTxService.publish(any(), any(), any()))
                .thenThrow(new PessimisticLockingFailureException("deadlock"));
        when(agreementBundleRepository.findByRequestId("req-1")).thenReturn(Optional.empty());

        assertCode(AgreementErrorCode.AGREEMENT_TEMPORARILY_UNAVAILABLE, () -> service.publish(cmd()));
    }

    private PublishAgreementBundleCmd cmd() {
        return PublishAgreementBundleCmd.builder()
                .scene("IVI_ACTIVATION")
                .privacyRevisionId("REV-P")
                .userAgreementRevisionId("REV-U")
                .requestId("req-1")
                .expectedBundleId(null)
                .expectedBundleIdPresent(true)
                .operator("tester")
                .build();
    }

    private AgreementBundle bundle(String requestHash) {
        return AgreementBundle.builder()
                .bundleId("BDL-EXISTING")
                .scope(DEFAULT_SCOPE)
                .privacyRevisionId("REV-P")
                .userRevisionId("REV-U")
                .publishedBy("tester")
                .publishedAt(NOW)
                .requestId("req-1")
                .requestHash(requestHash)
                .build();
    }

    private void assertCode(AgreementErrorCode expected, Runnable action) {
        AgreementException exception = assertThrows(AgreementException.class, action::run);
        assertEquals(expected, exception.getErrorCode());
    }

    private static final class DefaultOnlyScopeProvider implements SupportedScopeProvider {
        @Override
        public Set<String> supportedMarkets() {
            return Set.of("DEFAULT");
        }

        @Override
        public Set<String> supportedLocales() {
            return Set.of("zh-CN");
        }

        @Override
        public Set<String> supportedBrands() {
            return Set.of("DEFAULT");
        }

        @Override
        public Set<String> supportedCarLines() {
            return Set.of("DEFAULT");
        }

        @Override
        public boolean isCarLineOwnedByBrand(String brandCode, String carLineCode) {
            return "DEFAULT".equals(carLineCode);
        }
    }
}
