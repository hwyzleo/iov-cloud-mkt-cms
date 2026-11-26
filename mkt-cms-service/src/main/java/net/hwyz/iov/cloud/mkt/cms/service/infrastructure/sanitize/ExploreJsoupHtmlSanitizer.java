package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.sanitize;

import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.ExploreHtmlSanitizer;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 探索页官方文章富文本清洗（jsoup 解析 + 允许列表）。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.3 / D-012 / AT-018：
 * <ul>
 *   <li>允许段落、标题、强调、列表、安全链接与受控图片；</li>
 *   <li>危险节点（脚本、样式、嵌入、表单、SVG、音视频等）连同载荷整体移除；</li>
 *   <li>属性默认全部禁止；链接仅 href/title 且 href 必须为绝对 HTTPS；图片仅 src/alt/width/height 且 src 命中素材白名单；</li>
 *   <li>确定性序列化（关闭美化输出、固定转义模式），校验输入/输出大小与可见内容；</li>
 *   <li>不使用正则表达式充当 HTML 安全解析器。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
public class ExploreJsoupHtmlSanitizer implements ExploreHtmlSanitizer {

    private static final String REPLACEMENT_CHARACTER = "\uFFFD";

    private static final String NON_BREAKING_SPACE = "\u00A0";

    private static final Set<String> DANGEROUS_TAGS;

    static {
        Set<String> tags = new LinkedHashSet<>();
        Collections.addAll(tags,
                "script", "style", "iframe", "frame", "frameset", "object", "embed", "applet",
                "form", "input", "button", "textarea", "select", "option", "optgroup", "label",
                "picture", "source", "video", "audio", "track", "map", "area", "canvas",
                "svg", "math", "link", "meta", "base", "template", "noscript", "title", "head",
                "marquee", "plaintext", "xml", "dialog", "slot", "portal", "fencedframe");
        DANGEROUS_TAGS = Collections.unmodifiableSet(tags);
    }

    private final ExploreHtmlPolicy policy;

    private final ExploreMediaValidator mediaValidator;

    public ExploreJsoupHtmlSanitizer(ExploreHtmlPolicy policy, ExploreMediaValidator mediaValidator) {
        this.policy = policy;
        this.mediaValidator = mediaValidator;
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
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "清洗后文章正文超过上限 " + policy.getMaxOutputBytes() + " 字节");
        }
        if (isVisibleContentEmpty(document.body())) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "清洗后文章正文没有可见内容");
        }
        return SanitizedHtml.builder()
                .body(body)
                .sanitizerVersion(policy.sanitizerVersion())
                .adjusted(!body.equals(normalizedInput))
                .build();
    }

    private String validateAndNormalize(String rawHtml) {
        if (rawHtml == null) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "文章正文不能为空");
        }
        if (rawHtml.indexOf(REPLACEMENT_CHARACTER) >= 0) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "文章正文不是合法 UTF-8 文本");
        }
        String normalized = rawHtml.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.trim().isEmpty()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "文章正文不能为空");
        }
        if (byteLength(normalized) > policy.getMaxInputBytes()) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT,
                    "文章正文超过上限 " + policy.getMaxInputBytes() + " 字节");
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
        for (Attribute attribute : new ArrayList<>(element.attributes().asList())) {
            String key = attribute.getKey().toLowerCase(Locale.ROOT);
            if (ExploreHtmlPolicy.LINK_TAG.equals(tagName)) {
                if (ExploreHtmlPolicy.LINK_HREF_ATTRIBUTE.equals(key)) {
                    if (!isAbsoluteHttps(element.attr(attribute.getKey()))) {
                        element.removeAttr(attribute.getKey());
                    }
                } else if (!ExploreHtmlPolicy.LINK_TITLE_ATTRIBUTE.equals(key)) {
                    element.removeAttr(attribute.getKey());
                }
            } else if (ExploreHtmlPolicy.IMAGE_TAG.equals(tagName)) {
                if (ExploreHtmlPolicy.IMAGE_SRC_ATTRIBUTE.equals(key)) {
                    if (!isAllowedImageSource(element.attr(attribute.getKey()))) {
                        element.removeAttr(attribute.getKey());
                    }
                } else if (!ExploreHtmlPolicy.IMAGE_ALT_ATTRIBUTE.equals(key)
                        && !"width".equals(key) && !"height".equals(key)) {
                    element.removeAttr(attribute.getKey());
                }
            } else {
                element.removeAttr(attribute.getKey());
            }
        }
        if (ExploreHtmlPolicy.IMAGE_TAG.equals(tagName) && !element.hasAttr(ExploreHtmlPolicy.IMAGE_SRC_ATTRIBUTE)) {
            element.remove();
        }
    }

    private boolean isAbsoluteHttps(String href) {
        if (href == null || href.isEmpty() || href.startsWith("//")) {
            return false;
        }
        String trimmed = href.trim();
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (Character.isWhitespace(current) || Character.isISOControl(current) || current == '\\') {
                return false;
            }
        }
        try {
            URI uri = new URI(trimmed);
            return uri.isAbsolute()
                    && ExploreHtmlPolicy.ALLOWED_PROTOCOL.equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isAllowedImageSource(String src) {
        if (!isAbsoluteHttps(src)) {
            return false;
        }
        try {
            mediaValidator.validateAssetUrl(src, "文章图片");
            return true;
        } catch (ExploreException e) {
            return false;
        }
    }

    private boolean isVisibleContentEmpty(Element body) {
        String text = body.text().replace(NON_BREAKING_SPACE, " ");
        return text.trim().isEmpty() && body.select(ExploreHtmlPolicy.IMAGE_TAG).isEmpty();
    }

    private int byteLength(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
