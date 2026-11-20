package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 协议正文清洗配置（大小上限）。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1.1：输入与输出各不超过上限，建议 1 MiB；
 * 端侧联调期间可调整，但不允许无上限。
 *
 * @author hwyz_leo
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cms.agreement.html")
public class AgreementHtmlProperties {

    /**
     * 输入正文上限（字节）
     */
    private int maxInputBytes = 1024 * 1024;

    /**
     * 输出正文上限（字节）
     */
    private int maxOutputBytes = 1024 * 1024;
}
