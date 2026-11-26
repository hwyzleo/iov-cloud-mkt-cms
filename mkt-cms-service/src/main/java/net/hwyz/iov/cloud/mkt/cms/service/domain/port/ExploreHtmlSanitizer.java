package net.hwyz.iov.cloud.mkt.cms.service.domain.port;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;

/**
 * 探索页官方文章富文本清洗端口。
 *
 * @author hwyz_leo
 */
public interface ExploreHtmlSanitizer {

    /**
     * 解析、清洗并确定性序列化官方文章正文
     *
     * @param rawHtml 原始 HTML 片段
     * @return 清洗结果（正文、策略版本、是否调整）
     */
    SanitizedHtml sanitize(String rawHtml);
}
