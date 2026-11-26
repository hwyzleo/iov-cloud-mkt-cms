package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.sanitize;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 探索页官方文章富文本清洗测试（MKT-CMS-DSN-CR-003 §4.3 / AT-018）。
 *
 * @author hwyz_leo
 */
@DisplayName("探索页富文本 - 允许列表/危险节点/受控图片")
class ExploreJsoupHtmlSanitizerTest {

    private ExploreJsoupHtmlSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        ExploreMediaValidator mediaValidator = new ExploreMediaValidator(
                Set.of("assets.example"), Set.of("https://h5.example"),
                Set.of("articleDetail"), Set.of("id"));
        sanitizer = new ExploreJsoupHtmlSanitizer(
                new ExploreHtmlPolicy(1024 * 1024, 1024 * 1024), mediaValidator);
    }

    @Test
    @DisplayName("危险节点与事件属性整体移除，正常格式保留")
    void 危险节点移除() {
        SanitizedHtml sanitized = sanitizer.sanitize(
                "<p onclick=\"alert(1)\">正文<strong>加粗</strong></p>"
                        + "<script>alert(2)</script><iframe src=\"https://evil\"></iframe>"
                        + "<form><input name=\"x\"></form>");
        assertFalse(sanitized.getBody().contains("script"));
        assertFalse(sanitized.getBody().contains("iframe"));
        assertFalse(sanitized.getBody().contains("input"));
        assertFalse(sanitized.getBody().contains("onclick"));
        assertTrue(sanitized.getBody().contains("<p>正文<strong>加粗</strong></p>"));
        assertTrue(sanitized.isAdjusted());
    }

    @Test
    @DisplayName("链接仅保留绝对 HTTPS，危险协议降级为文本")
    void 链接安全() {
        SanitizedHtml sanitized = sanitizer.sanitize(
                "<p><a href=\"https://h5.example/a\" title=\"t\">安全</a>"
                        + "<a href=\"javascript:alert(1)\">危险</a>"
                        + "<a href=\"//evil.example/x\">相对</a></p>");
        assertTrue(sanitized.getBody().contains("href=\"https://h5.example/a\""));
        assertTrue(sanitized.getBody().contains("<a>危险</a>"));
        assertTrue(sanitized.getBody().contains("<a>相对</a>"));
        assertFalse(sanitized.getBody().contains("javascript"));
    }

    @Test
    @DisplayName("图片仅保留白名单 HTTPS 域名与宽高，其他图片移除")
    void 受控图片() {
        SanitizedHtml sanitized = sanitizer.sanitize(
                "<p>图文</p><img src=\"https://assets.example/a.webp\" alt=\"封面\" width=\"800\" height=\"400\">"
                        + "<img src=\"https://evil.example/b.png\"><img src=\"http://assets.example/c.png\">");
        assertTrue(sanitized.getBody().contains("src=\"https://assets.example/a.webp\""));
        assertTrue(sanitized.getBody().contains("width=\"800\""));
        assertFalse(sanitized.getBody().contains("evil.example"));
        assertFalse(sanitized.getBody().contains("http://"));
    }

    @Test
    @DisplayName("确定性序列化：重复清洗结果一致，不美化输出")
    void 确定性序列化() {
        String raw = "<div><p>正文</p></div>";
        SanitizedHtml first = sanitizer.sanitize(raw);
        SanitizedHtml second = sanitizer.sanitize(first.getBody());
        assertEquals(first.getBody(), second.getBody());
        assertFalse(first.getBody().contains("\n"));
        assertEquals(ExploreHtmlPolicy.SANITIZER_VERSION, first.getSanitizerVersion());
    }

    @Test
    @DisplayName("清洗后无可见内容或超限时拒绝")
    void 空正文与超限() {
        assertThrows(ExploreException.class, () -> sanitizer.sanitize("<script>alert(1)</script>"));
        ExploreJsoupHtmlSanitizer small = new ExploreJsoupHtmlSanitizer(
                new ExploreHtmlPolicy(10, 10), new ExploreMediaValidator(
                Set.of("assets.example"), Set.of(), Set.of(), Set.of()));
        assertThrows(ExploreException.class, () -> small.sanitize("<p>超过十个字节的正文</p>"));
    }
}
