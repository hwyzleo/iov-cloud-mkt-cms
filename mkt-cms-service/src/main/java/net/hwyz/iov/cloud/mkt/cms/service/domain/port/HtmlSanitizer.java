package net.hwyz.iov.cloud.mkt.cms.service.domain.port;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;

/**
 * 富文本清洗端口（域层定义，基础设施层以成熟解析器实现）。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1.1：先校验 UTF-8 与输入大小，再解析清洗和确定性序列化；
 * 校验输出大小与可见正文非空；禁止用正则表达式充当 HTML 安全解析器。
 *
 * @author hwyz_leo
 */
public interface HtmlSanitizer {

    /**
     * 清洗并确定性序列化协议正文
     *
     * @param rawHtml 原始 HTML 片段
     * @return 清洗后的安全快照；输入非法时抛 {@code AGREEMENT_INVALID_ARGUMENT}
     */
    SanitizedHtml sanitize(String rawHtml);
}
