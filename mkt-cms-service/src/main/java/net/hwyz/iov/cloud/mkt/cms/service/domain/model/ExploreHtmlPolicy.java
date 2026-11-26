package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 探索页官方文章富文本允许列表策略与大小上限。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.3 / D-012：允许段落、标题、强调、列表、安全链接和受控图片；
 * 禁止脚本、事件属性、iframe、SVG、表单、自动播放与不受控外部资源；
 * 图片仅允许 HTTPS 且域名命中服务端白名单。
 *
 * @author hwyz_leo
 */
@Getter
public final class ExploreHtmlPolicy {

    /**
     * 首版清洗策略版本（策略升级须新建版本号并保留旧修订）
     */
    public static final String SANITIZER_VERSION = "explore-html-allowlist-v1";

    /**
     * 链接标签
     */
    public static final String LINK_TAG = "a";

    /**
     * 图片标签
     */
    public static final String IMAGE_TAG = "img";

    /**
     * 链接地址属性
     */
    public static final String LINK_HREF_ATTRIBUTE = "href";

    /**
     * 链接标题属性
     */
    public static final String LINK_TITLE_ATTRIBUTE = "title";

    /**
     * 图片地址属性
     */
    public static final String IMAGE_SRC_ATTRIBUTE = "src";

    /**
     * 图片替代文本属性
     */
    public static final String IMAGE_ALT_ATTRIBUTE = "alt";

    /**
     * 唯一允许的协议
     */
    public static final String ALLOWED_PROTOCOL = "https";

    private static final Set<String> ALLOWED_TAGS;

    static {
        Set<String> tags = new LinkedHashSet<>();
        Collections.addAll(tags, "p", "br", "h1", "h2", "h3", "h4",
                "strong", "em", "u", "ol", "ul", "li", LINK_TAG, IMAGE_TAG);
        ALLOWED_TAGS = Collections.unmodifiableSet(tags);
    }

    private final int maxInputBytes;

    private final int maxOutputBytes;

    public ExploreHtmlPolicy(int maxInputBytes, int maxOutputBytes) {
        if (maxInputBytes <= 0 || maxOutputBytes <= 0) {
            throw new IllegalArgumentException("探索页正文大小上限必须为正数");
        }
        this.maxInputBytes = maxInputBytes;
        this.maxOutputBytes = maxOutputBytes;
    }

    /**
     * 允许的标签集合（小写）
     *
     * @return 不可变允许标签集合
     */
    public Set<String> allowedTags() {
        return ALLOWED_TAGS;
    }

    /**
     * 标签是否在允许列表中
     *
     * @param tagName 小写标签名
     * @return 是否允许
     */
    public boolean isAllowedTag(String tagName) {
        return ALLOWED_TAGS.contains(tagName);
    }

    /**
     * 清洗策略版本
     *
     * @return 固定版本号
     */
    public String sanitizerVersion() {
        return SANITIZER_VERSION;
    }
}
