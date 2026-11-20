package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 协议富文本允许列表策略与大小上限。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1.1：允许标签固定为 p、br、h1～h4、strong、em、u、ol、ul、li、a；
 * 属性默认全部禁止，仅链接允许 href、title，href 仅接受绝对 HTTPS 链接；
 * 输入与输出各不超过上限（默认 1 MiB），清洗策略版本固定，升级不修改旧修订。
 *
 * @author hwyz_leo
 */
@Getter
public final class AgreementHtmlPolicy {

    /**
     * 首版清洗策略版本（固定值；策略升级须新建版本号并保留旧修订）
     */
    public static final String SANITIZER_VERSION = "html-allowlist-v1";

    /**
     * 允许的链接属性
     */
    public static final String LINK_TAG = "a";

    /**
     * 链接允许的唯一地址属性
     */
    public static final String LINK_HREF_ATTRIBUTE = "href";

    /**
     * 链接允许的标题属性
     */
    public static final String LINK_TITLE_ATTRIBUTE = "title";

    /**
     * 链接允许的唯一协议
     */
    public static final String LINK_ALLOWED_PROTOCOL = "https";

    private static final Set<String> ALLOWED_TAGS;

    static {
        Set<String> tags = new LinkedHashSet<>();
        Collections.addAll(tags, "p", "br", "h1", "h2", "h3", "h4",
                "strong", "em", "u", "ol", "ul", "li", LINK_TAG);
        ALLOWED_TAGS = Collections.unmodifiableSet(tags);
    }

    private final int maxInputBytes;

    private final int maxOutputBytes;

    public AgreementHtmlPolicy(int maxInputBytes, int maxOutputBytes) {
        if (maxInputBytes <= 0 || maxOutputBytes <= 0) {
            throw new IllegalArgumentException("协议正文大小上限必须为正数");
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
