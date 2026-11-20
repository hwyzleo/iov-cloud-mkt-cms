package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 清洗后的安全 HTML 快照。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1.1：草稿响应必须返回清洗后的实际正文、清洗策略版本与是否发生调整，
 * 调用方必须展示保存结果供维护人员核对，不得用清洗前原文预览或发布。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class SanitizedHtml {

    /**
     * 首版正文格式（HTML 片段，不是完整网页）
     */
    public static final String CONTENT_TYPE = "text/html; charset=utf-8";

    /**
     * 清洗后的确定性序列化正文
     */
    private final String body;

    /**
     * 清洗策略版本
     */
    private final String sanitizerVersion;

    /**
     * 是否相对输入发生了调整（有标签/属性被移除或降级）
     */
    private final boolean adjusted;
}
