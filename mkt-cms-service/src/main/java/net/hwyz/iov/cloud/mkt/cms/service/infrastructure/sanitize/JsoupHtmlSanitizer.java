package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.sanitize;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.HtmlSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.safety.Safelist;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 基于 jsoup 解析器与允许列表的安全 HTML 清洗实现。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1.1：
 * <ul>
 *   <li>危险节点（脚本、样式、嵌入、表单、图片、SVG、音视频等）连同载荷整体移除；</li>
 *   <li>不在允许列表中的普通格式标签降级为可见文本（unwrap），不丢内容；</li>
 *   <li>属性默认全部禁止，仅链接允许 href、title，且 href 只接受绝对 HTTPS 链接；</li>
 *   <li>先校验 UTF-8 与输入大小，再解析清洗与确定性序列化（关闭美化输出，固定转义模式），
 *       最后校验输出大小与可见正文非空；</li>
 *   <li>不使用正则表达式充当 HTML 安全解析器。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
public class JsoupHtmlSanitizer implements HtmlSanitizer {

    /**
     * UTF-8 解码替换符：出现即表示上游字节序列非法
     */
    private static final String REPLACEMENT_CHARACTER = "\uFFFD";

    /**
     * 不换行空格（用于可见正文判空）
     */
    private static final String NON_BREAKING_SPACE = "\u00A0";

    private static final Set<String> DANGEROUS_TAGS;

    static {
        Set<String> tags = new LinkedHashSet<>();
        Collections.addAll(tags,
                "script", "style", "iframe", "frame", "frameset", "object", "embed", "applet",
                "form", "input", "button", "textarea", "select", "option", "optgroup", "label",
                "img", "picture", "source", "video", "audio", "track", "map", "area", "canvas",
                "svg", "math", "link", "meta", "base", "template", "noscript", "title", "head",
                "marquee", "plaintext", "xml", "dialog", "slot", "portal", "fencedframe");
        DANGEROUS_TAGS = Collections.unmodifiableSet(tags);
    }

    private final AgreementHtmlPolicy policy;

    private final Safelist safelist;

    public JsoupHtmlSanitizer(AgreementHtmlPolicy policy) {
        this.policy = policy;
        this.safelist = buildSafelist(policy);
    }

    @Override
    public SanitizedHtml sanitize(String rawHtml) {
        String normalizedInput = validateAndNormalize(rawHtml);
        Document document = Jsoup.parseBodyFragment(normalizedInput);
        document.outputSettings(new Document.OutputSettings()
                .prettyPrint(false)
                .escapeMode(Entities.EscapeMode.base)
                .charset(StandardCharsets.UTF_8));
        cleanChildren(document.body());

        String body = document.body().html();
        if (byteLength(body) > policy.getMaxOutputBytes()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "清洗后协议正文超过上限 " + policy.getMaxOutputBytes() + " 字节");
        }
        if (isVisibleTextEmpty(document.body())) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "清洗后协议正文没有可见内容");
        }
        return SanitizedHtml.builder()
                .body(body)
                .sanitizerVersion(policy.sanitizerVersion())
                .adjusted(!body.equals(normalizedInput))
                .build();
    }

    private String validateAndNormalize(String rawHtml) {
        if (rawHtml == null) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议正文不能为空");
        }
        if (rawHtml.indexOf(REPLACEMENT_CHARACTER) >= 0) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议正文不是合法 UTF-8 文本");
        }
        String normalized = rawHtml.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议正文不能为空");
        }
        if (byteLength(normalized) > policy.getMaxInputBytes()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "协议正文超过上限 " + policy.getMaxInputBytes() + " 字节");
        }
        return normalized;
    }

    private void cleanChildren(Element parent) {
        for (Node child : new ArrayList<>(parent.childNodes())) {
            if (child instanceof Comment) {
                child.remove();
                continue;
            }
            if (child instanceof DataNode) {
                child.remove();
                continue;
            }
            if (!(child instanceof Element element)) {
                continue;
            }
            String tagName = element.tagName().toLowerCase(Locale.ROOT);
            if (DANGEROUS_TAGS.contains(tagName)) {
                element.remove();
                continue;
            }
            if (!policy.isAllowedTag(tagName)) {
                cleanChildren(element);
                element.unwrap();
                continue;
            }
            cleanAttributes(tagName, element);
            cleanChildren(element);
        }
    }

    private void cleanAttributes(String tagName, Element element) {
        List<Attribute> attributes = new ArrayList<>(element.attributes().asList());
        for (Attribute attribute : attributes) {
            if (!safelist.isSafeAttribute(tagName, element, attribute)) {
                element.removeAttr(attribute.getKey());
            }
        }
        if (AgreementHtmlPolicy.LINK_TAG.equals(tagName) && element.hasAttr(AgreementHtmlPolicy.LINK_HREF_ATTRIBUTE)) {
            String href = element.attr(AgreementHtmlPolicy.LINK_HREF_ATTRIBUTE).trim();
            if (!isAbsoluteHttps(href)) {
                element.removeAttr(AgreementHtmlPolicy.LINK_HREF_ATTRIBUTE);
            }
        }
    }

    /**
     * 是否为绝对 HTTPS 链接（拒绝相对、协议相对、javascript/data/file 等协议）
     */
    private boolean isAbsoluteHttps(String href) {
        if (href.isEmpty() || href.startsWith("//")) {
            return false;
        }
        String lower = href.toLowerCase(Locale.ROOT);
        if (!lower.startsWith(AgreementHtmlPolicy.LINK_ALLOWED_PROTOCOL + "://")) {
            return false;
        }
        for (int index = 0; index < href.length(); index++) {
            char current = href.charAt(index);
            if (Character.isWhitespace(current) || Character.isISOControl(current) || current == '\\') {
                return false;
            }
        }
        try {
            URI uri = URI.create(href);
            return uri.isAbsolute()
                    && AgreementHtmlPolicy.LINK_ALLOWED_PROTOCOL.equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isEmpty();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isVisibleTextEmpty(Element body) {
        String text = body.text().replace(NON_BREAKING_SPACE, " ");
        return text.trim().isEmpty();
    }

    private int byteLength(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static Safelist buildSafelist(AgreementHtmlPolicy policy) {
        Safelist allow = new Safelist();
        for (String tag : policy.allowedTags()) {
            if (AgreementHtmlPolicy.LINK_TAG.equals(tag)) {
                allow.addTags(tag)
                        .addAttributes(tag, AgreementHtmlPolicy.LINK_HREF_ATTRIBUTE,
                                AgreementHtmlPolicy.LINK_TITLE_ATTRIBUTE)
                        .addProtocols(tag, AgreementHtmlPolicy.LINK_HREF_ATTRIBUTE,
                                AgreementHtmlPolicy.LINK_ALLOWED_PROTOCOL);
            } else {
                allow.addTags(tag);
            }
        }
        return allow;
    }
}
