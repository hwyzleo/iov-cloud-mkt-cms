package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.sanitize;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议富文本清洗单元测试（MKT-CMS-DSN-CR-001 §3.1.1 / AT-001 / AT-002）。
 *
 * @author hwyz_leo
 */
@DisplayName("协议富文本 - 允许列表/危险内容/确定性序列化/上限")
class JsoupHtmlSanitizerTest {

    private static final int ONE_MIB = 1024 * 1024;

    private final JsoupHtmlSanitizer sanitizer =
            new JsoupHtmlSanitizer(new AgreementHtmlPolicy(ONE_MIB, ONE_MIB));

    @Test
    @DisplayName("允许标签与安全链接保持可见且内容不丢")
    void 允许标签保留() {
        String input = "<h1>隐私政策</h1><p>请<strong>仔细</strong><em>阅读</em><u>本协议</u>"
                + "<br>生效日期见下</p><ul><li>第一项</li><li>第二项<ol><li>子项</li></ol></li></ul>"
                + "<p><a href=\"https://example.com/privacy\" title=\"隐私政策\">查看详情</a></p>";

        SanitizedHtml sanitized = sanitizer.sanitize(input);

        assertTrue(sanitized.getBody().contains("<h1>隐私政策</h1>"));
        assertTrue(sanitized.getBody().contains("<strong>仔细</strong>"));
        assertTrue(sanitized.getBody().contains("<ol><li>子项</li></ol>"));
        assertTrue(sanitized.getBody().contains("href=\"https://example.com/privacy\""));
        assertTrue(sanitized.getBody().contains("title=\"隐私政策\""));
        assertEquals(AgreementHtmlPolicy.SANITIZER_VERSION, sanitized.getSanitizerVersion());
        assertEquals(SanitizedHtml.CONTENT_TYPE, "text/html; charset=utf-8");
    }

    @Test
    @DisplayName("危险节点连同载荷整体移除")
    void 危险节点移除() {
        String input = "<p>正文</p><script>alert('x')</script><style>body{display:none}</style>"
                + "<iframe src=\"https://evil.example\"></iframe><object data=\"x.swf\"></object>"
                + "<embed src=\"x.swf\"><img src=\"x.png\" onerror=\"alert(1)\">"
                + "<svg><script>alert(1)</script></svg><form action=\"x\"><input name=\"a\"></form>"
                + "<video src=\"x.mp4\"></video><math><mi>x</mi></math>";

        SanitizedHtml sanitized = sanitizer.sanitize(input);

        String body = sanitized.getBody();
        assertTrue(body.contains("<p>正文</p>"));
        assertFalse(body.toLowerCase().contains("script"));
        assertFalse(body.toLowerCase().contains("alert"));
        assertFalse(body.toLowerCase().contains("<style"));
        assertFalse(body.toLowerCase().contains("iframe"));
        assertFalse(body.toLowerCase().contains("<img"));
        assertFalse(body.toLowerCase().contains("<svg"));
        assertFalse(body.toLowerCase().contains("<form"));
        assertFalse(body.toLowerCase().contains("<video"));
        assertFalse(body.toLowerCase().contains("<math"));
        assertTrue(sanitized.isAdjusted());
    }

    @Test
    @DisplayName("普通不支持格式降级为可见文本")
    void 不支持格式降级为文本() {
        SanitizedHtml sanitized = sanitizer.sanitize("<div><span>降级文本</span></div>");

        assertEquals("降级文本", sanitized.getBody());
    }

    @Test
    @DisplayName("事件属性、style/class/id 与未知属性被移除")
    void 属性白名单() {
        SanitizedHtml sanitized = sanitizer.sanitize(
                "<p style=\"color:red\" class=\"c\" id=\"i\" onclick=\"alert(1)\" data-x=\"1\">正文</p>");

        assertEquals("<p>正文</p>", sanitized.getBody());
    }

    @Test
    @DisplayName("危险或非绝对 HTTPS 链接移除链接能力但保留可见文字")
    void 危险链接降级() {
        for (String href : new String[]{"javascript:alert(1)", "JAVASCRIPT:alert(1)", "data:text/html,<b>x</b>",
                "/relative/path", "//example.com/x", "http://example.com/x", "file:///etc/passwd",
                "https:///nohost", "https://exa mple.com/x"}) {
            SanitizedHtml sanitized = sanitizer.sanitize("<p><a href=\"" + href + "\">可见文字</a></p>");
            assertFalse(sanitized.getBody().contains("href"), "危险链接未被移除: " + href);
            assertTrue(sanitized.getBody().contains("可见文字"), "可见文字丢失: " + href);
        }
    }

    @Test
    @DisplayName("实体与编码混淆的链接协议不能存活为可执行链接")
    void 编码混淆链接() {
        SanitizedHtml sanitized = sanitizer.sanitize(
                "<p><a href=\"&#106;avascript&#58;alert(1)\">文字</a></p>");

        assertFalse(sanitized.getBody().contains("href"));
        assertTrue(sanitized.getBody().contains("文字"));
    }

    @Test
    @DisplayName("确定性序列化：同一输入重复清洗结果稳定，换行统一 LF")
    void 确定性序列化() {
        String input = "<p>第一行\r\n第二行</p>\r\n<p>第三行</p>";

        SanitizedHtml first = sanitizer.sanitize(input);
        SanitizedHtml second = sanitizer.sanitize(input);

        assertEquals(first.getBody(), second.getBody());
        assertFalse(first.getBody().contains("\r"));
        assertTrue(first.getBody().contains("第一行\n第二行"));
    }

    @Test
    @DisplayName("中文、实体与嵌套列表在清洗与回读中保持一致")
    void 中文与实体一致() {
        String input = "<p>协议&nbsp;条款 &amp; 附件</p><ul><li>甲<ol><li>乙</li></ol></li></ul>";
        SanitizedHtml sanitized = sanitizer.sanitize(input);

        assertEquals(sanitized.getBody(), sanitizer.sanitize(sanitized.getBody()).getBody());
        assertTrue(sanitized.getBody().contains("协议"));
        assertTrue(sanitized.getBody().contains("&amp;"));
    }

    @Test
    @DisplayName("清洗后无可见正文拒绝")
    void 空正文拒绝() {
        AgreementException empty = assertThrows(AgreementException.class, () -> sanitizer.sanitize("   "));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, empty.getErrorCode());

        AgreementException tagOnly = assertThrows(AgreementException.class,
                () -> sanitizer.sanitize("<p><br></p><script>alert(1)</script>"));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, tagOnly.getErrorCode());

        assertThrows(AgreementException.class, () -> sanitizer.sanitize(null));
    }

    @Test
    @DisplayName("输入与输出大小上限拒绝")
    void 大小上限() {
        AgreementHtmlPolicy tiny = new AgreementHtmlPolicy(32, 16);
        JsoupHtmlSanitizer tinySanitizer = new JsoupHtmlSanitizer(tiny);

        AgreementException tooLargeInput = assertThrows(AgreementException.class,
                () -> tinySanitizer.sanitize("<p>" + "甲".repeat(40) + "</p>"));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, tooLargeInput.getErrorCode());

        AgreementException tooLargeOutput = assertThrows(AgreementException.class,
                () -> tinySanitizer.sanitize("<p>" + "甲".repeat(8) + "</p>"));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, tooLargeOutput.getErrorCode());
    }

    @Test
    @DisplayName("非法 UTF-8 字节（解码替换符）拒绝")
    void 非法UTF8拒绝() {
        AgreementException exception = assertThrows(AgreementException.class,
                () -> sanitizer.sanitize("<p>正文\uFFFD</p>"));
        assertEquals(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, exception.getErrorCode());
    }

    @Test
    @DisplayName("未发生调整时 adjusted=false")
    void 未调整标记() {
        SanitizedHtml sanitized = sanitizer.sanitize("<p>正文</p>");

        assertFalse(sanitized.isAdjusted());
    }
}
