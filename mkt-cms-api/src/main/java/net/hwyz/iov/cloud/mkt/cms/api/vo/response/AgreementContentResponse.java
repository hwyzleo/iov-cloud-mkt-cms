package net.hwyz.iov.cloud.mkt.cms.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 单份协议内容出参（安全 HTML 不可变快照）。
 * <p>
 * MKT-CMS-DSN-CR-001 §6.1/§6.2：当前包与历史修订共用同一份协议内容结构；
 * {@code contentHash} 仅用于内容一致性校验，不是数字签名，也不是用户同意证明。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementContentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 稳定协议身份标识，由服务端按协议身份确定性派生
     */
    private String agreementCode;

    /**
     * 协议类型：PRIVACY_POLICY / USER_AGREEMENT
     */
    private String agreementType;

    /**
     * 不可变修订标识
     */
    private String revisionId;

    /**
     * 业务展示版本，不作为排序键
     */
    private String version;

    /**
     * 内容提供方确认的协议日期（YYYY-MM-DD）
     */
    private LocalDate agreementDate;

    /**
     * 协议标题
     */
    private String title;

    /**
     * 场景
     */
    private String scene;

    /**
     * 市场
     */
    private String market;

    /**
     * 语言
     */
    private String locale;

    /**
     * 品牌
     */
    private String brandCode;

    /**
     * 车系
     */
    private String carLineCode;

    /**
     * 正文格式，首版固定 text/html; charset=utf-8
     */
    private String contentType;

    /**
     * 清洗后的安全 HTML 片段
     */
    private String body;

    /**
     * 正文精确字节的 SHA-256，形如 sha256:&lt;64 位小写十六进制&gt;
     */
    private String contentHash;

    /**
     * 清洗策略版本
     */
    private String sanitizerVersion;
}
