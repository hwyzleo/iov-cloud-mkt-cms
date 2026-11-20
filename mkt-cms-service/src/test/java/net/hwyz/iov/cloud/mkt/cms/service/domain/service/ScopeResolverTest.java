package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议范围解析单元测试（MKT-CMS-DSN-CR-001 §3.1 / AT-005）。
 *
 * @author hwyz_leo
 */
@DisplayName("协议范围 - 默认补齐/显式空值拒绝/不受支持值拒绝/车系归属")
class ScopeResolverTest {

    private final ScopeResolver resolver = new ScopeResolver(new StubSupportedScopeProvider());

    @Test
    @DisplayName("仅传场景时补齐 DEFAULT/zh-CN/DEFAULT/DEFAULT 并显式返回")
    void 默认补齐() {
        AgreementScope scope = resolver.resolve("IVI_ACTIVATION", null, null, null, null);

        assertEquals(AgreementScene.IVI_ACTIVATION, scope.getScene());
        assertEquals("DEFAULT", scope.getMarket());
        assertEquals("zh-CN", scope.getLocale());
        assertEquals("DEFAULT", scope.getBrandCode());
        assertEquals("DEFAULT", scope.getCarLineCode());
    }

    @Test
    @DisplayName("显式默认与省略默认得到同一规范化范围")
    void 显式默认等价于省略() {
        AgreementScope omitted = resolver.resolve("IVI_ACTIVATION", null, null, null, null);
        AgreementScope explicit = resolver.resolve("IVI_ACTIVATION", "DEFAULT", "zh-CN", "DEFAULT", "DEFAULT");

        assertEquals(omitted, explicit);
        assertEquals(omitted.identityKey(), explicit.identityKey());
    }

    @Test
    @DisplayName("场景必传，空值或不受支持值拒绝")
    void 场景必传() {
        assertInvalid(() -> resolver.resolve(null, null, null, null, null));
        assertInvalid(() -> resolver.resolve(" ", null, null, null, null));
        assertInvalid(() -> resolver.resolve("APP_REGISTER", null, null, null, null));
    }

    @Test
    @DisplayName("显式空串拒绝，不被悄悄替换为默认值")
    void 显式空值拒绝() {
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", "", null, null, null));
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, "  ", null, null));
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, null, "", null));
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, null, null, ""));
    }

    @Test
    @DisplayName("不受支持的语言/市场拒绝，不跨语言跨市场回退")
    void 不受支持值拒绝() {
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, "en-US", null, null));
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", "CN", null, null, null));
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, null, "BMW", null));
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, null, null, "CL1"));
    }

    @Test
    @DisplayName("品牌为 DEFAULT 时车系只能为 DEFAULT")
    void 品牌默认车系约束() {
        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, null, "DEFAULT", "CL1"));
    }

    @Test
    @DisplayName("具体车系必须归属具体品牌；品牌级（车系 DEFAULT）合法")
    void 车系归属校验() {
        AgreementScope brandLevel = resolver.resolve("IVI_ACTIVATION", null, null, "BM", null);
        assertEquals("BM", brandLevel.getBrandCode());
        assertEquals("DEFAULT", brandLevel.getCarLineCode());

        AgreementScope owned = resolver.resolve("IVI_ACTIVATION", null, null, "BM", "CL1");
        assertEquals("CL1", owned.getCarLineCode());

        assertInvalid(() -> resolver.resolve("IVI_ACTIVATION", null, null, "BM", "CL2"));
    }

    @Test
    @DisplayName("不同范围的标识互不相同（完整五维参与身份）")
    void 身份五维参与() {
        AgreementScope base = resolver.resolve("IVI_ACTIVATION", null, null, null, null);
        AgreementScope brand = resolver.resolve("IVI_ACTIVATION", null, null, "BM", null);

        assertTrue(!base.identityKey().equals(brand.identityKey()));
    }

    private void assertInvalid(Runnable action) {
        AgreementException exception = assertThrows(AgreementException.class, action::run);
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, exception.getErrorCode());
    }

    /**
     * 受支持清单夹具：默认值 + 一个品牌级/车系级扩展，用于验证预留扩展能力
     */
    private static final class StubSupportedScopeProvider implements SupportedScopeProvider {

        private final Map<String, Set<String>> ownership = new LinkedHashMap<>();

        private StubSupportedScopeProvider() {
            ownership.put("BM", Set.of("CL1"));
            ownership.put("DEFAULT", Set.of("DEFAULT"));
        }

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
            return Set.of("DEFAULT", "BM");
        }

        @Override
        public Set<String> supportedCarLines() {
            return Set.of("DEFAULT", "CL1", "CL2");
        }

        @Override
        public boolean isCarLineOwnedByBrand(String brandCode, String carLineCode) {
            return ownership.getOrDefault(brandCode, Set.of()).contains(carLineCode);
        }
    }
}
