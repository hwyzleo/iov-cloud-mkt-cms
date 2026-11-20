package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发布命令哈希单元测试（MKT-CMS-DSN-CR-001 §5.1 / AT-004）。
 *
 * @author hwyz_leo
 */
@DisplayName("发布命令哈希 - 显式默认等价/范围参与/前提参与")
class PublishRequestHashTest {

    private static final SupportedScopeProvider DEFAULT_SCOPE_PROVIDER = new SupportedScopeProvider() {
        @Override
        public Set<String> supportedMarkets() {
            return Set.of("DEFAULT", "CN");
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
    };

    private final ScopeResolver resolver = new ScopeResolver(DEFAULT_SCOPE_PROVIDER);

    @Test
    @DisplayName("显式默认与省略默认生成相同哈希")
    void 显式默认等价() {
        AgreementScope omitted = resolver.resolve("IVI_ACTIVATION", null, null, null, null);
        AgreementScope explicit = resolver.resolve("IVI_ACTIVATION", "DEFAULT", "zh-CN", "DEFAULT", "DEFAULT");

        assertEquals(PublishRequestHash.of(omitted, "REV-P", "REV-U", null),
                PublishRequestHash.of(explicit, "REV-P", "REV-U", null));
    }

    @Test
    @DisplayName("五维范围参与哈希：市场不同则哈希不同")
    void 范围参与哈希() {
        AgreementScope base = resolver.resolve("IVI_ACTIVATION", null, null, null, null);
        AgreementScope otherMarket = resolver.resolve("IVI_ACTIVATION", "CN", null, null, null);

        assertNotEquals(PublishRequestHash.of(base, "REV-P", "REV-U", null),
                PublishRequestHash.of(otherMarket, "REV-P", "REV-U", null));
    }

    @Test
    @DisplayName("修订与发布前提参与哈希")
    void 命令内容参与哈希() {
        AgreementScope scope = resolver.resolve("IVI_ACTIVATION", null, null, null, null);

        String base = PublishRequestHash.of(scope, "REV-P", "REV-U", null);
        assertNotEquals(base, PublishRequestHash.of(scope, "REV-P2", "REV-U", null));
        assertNotEquals(base, PublishRequestHash.of(scope, "REV-P", "REV-U2", null));
        assertNotEquals(base, PublishRequestHash.of(scope, "REV-P", "REV-U", "BDL-1"));
    }

    @Test
    @DisplayName("哈希为 64 位小写十六进制且可重复计算")
    void 哈希格式稳定() {
        AgreementScope scope = resolver.resolve("IVI_ACTIVATION", null, null, null, null);
        String hash = PublishRequestHash.of(scope, "REV-P", "REV-U", null);

        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(hash, PublishRequestHash.of(scope, "REV-P", "REV-U", null));
    }
}
