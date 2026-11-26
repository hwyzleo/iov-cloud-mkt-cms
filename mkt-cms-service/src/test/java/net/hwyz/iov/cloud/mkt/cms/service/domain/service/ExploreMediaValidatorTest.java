package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 探索页素材、H5 与 NATIVE 白名单校验测试（MKT-CMS-DSN-CR-003 §4.3 / AT-018）。
 *
 * @author hwyz_leo
 */
@DisplayName("探索页白名单 - 素材/H5/NATIVE/敏感参数")
class ExploreMediaValidatorTest {

    private final ExploreMediaValidator validator = new ExploreMediaValidator(
            Set.of("assets.example", "cdn.example:8443"),
            Set.of("https://h5.example"),
            Set.of("articleDetail"),
            Set.of("id", "tab", "token"));

    @Test
    @DisplayName("图片仅允许白名单 HTTPS 主机与 JPEG/PNG/WebP")
    void 图片校验() {
        assertDoesNotThrow(() -> validator.validateRequiredImage(image("https://assets.example/a.png",
                "image/png"), "封面"));
        assertDoesNotThrow(() -> validator.validateRequiredImage(image("https://cdn.example:8443/a.webp",
                "image/webp"), "封面"));
        assertThrows(ExploreException.class, () -> validator.validateRequiredImage(
                image("http://assets.example/a.png", "image/png"), "封面"));
        assertThrows(ExploreException.class, () -> validator.validateRequiredImage(
                image("https://evil.example/a.png", "image/png"), "封面"));
        assertThrows(ExploreException.class, () -> validator.validateRequiredImage(
                image("https://assets.example/a.gif", "image/gif"), "封面"));
    }

    @Test
    @DisplayName("H5 仅允许白名单 origin 的绝对 HTTPS")
    void H5校验() {
        assertDoesNotThrow(() -> validator.validateH5Url("https://h5.example/path?a=1"));
        assertThrows(ExploreException.class, () -> validator.validateH5Url("https://evil.example/path"));
        assertThrows(ExploreException.class, () -> validator.validateH5Url("/relative"));
        assertThrows(ExploreException.class, () -> validator.validateH5Url("javascript:alert(1)"));
    }

    @Test
    @DisplayName("NATIVE 仅允许白名单路由与参数键，拒绝敏感身份参数")
    void NATIVE校验() {
        assertDoesNotThrow(() -> validator.validateNative("articleDetail", Map.of("id", "1")));
        assertThrows(ExploreException.class,
                () -> validator.validateNative("unknownRoute", Map.of("id", "1")));
        assertThrows(ExploreException.class,
                () -> validator.validateNative("articleDetail", Map.of("unknown", "1")));
        assertThrows(ExploreException.class,
                () -> validator.validateNative("articleDetail", Map.of("token", "x")));
    }

    @Test
    @DisplayName("ARTICLE/TOPIC 目标必须包含业务标识与修订标识")
    void 目标结构() {
        assertDoesNotThrow(() -> validator.validateTarget(ExploreTarget.builder()
                .type(ExploreTargetType.ARTICLE).refId("ea_1").revisionId("ear_1").build()));
        assertThrows(ExploreException.class, () -> validator.validateTarget(ExploreTarget.builder()
                .type(ExploreTargetType.TOPIC).refId("et_1").build()));
        assertThrows(ExploreException.class, () -> validator.validateTarget(null));
    }

    private ExploreImage image(String url, String mime) {
        return ExploreImage.builder()
                .url(url)
                .width(800)
                .height(400)
                .mime(mime)
                .size(1024L)
                .build();
    }
}
