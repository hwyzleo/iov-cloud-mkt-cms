package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.HtmlSanitizer;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ScopeResolver;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.sanitize.JsoupHtmlSanitizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 协议子域域层对象装配。
 * <p>
 * 域层不依赖 Spring：策略、解析器与清洗器由本配置显式装配为 Bean。
 *
 * @author hwyz_leo
 */
@Configuration
public class AgreementDomainConfig {

    /**
     * 唯一的范围解析器（管理、发布、读取共用）
     *
     * @param supportedScopeProvider 受支持范围清单
     * @return 范围解析器
     */
    @Bean
    public ScopeResolver agreementScopeResolver(SupportedScopeProvider supportedScopeProvider) {
        return new ScopeResolver(supportedScopeProvider);
    }

    /**
     * 富文本清洗策略
     *
     * @param properties 清洗配置
     * @return 允许列表策略
     */
    @Bean
    public AgreementHtmlPolicy agreementHtmlPolicy(AgreementHtmlProperties properties) {
        return new AgreementHtmlPolicy(properties.getMaxInputBytes(), properties.getMaxOutputBytes());
    }

    /**
     * 富文本清洗器（jsoup 解析 + 允许列表）
     *
     * @param policy 清洗策略
     * @return 清洗端口实现
     */
    @Bean
    public HtmlSanitizer agreementHtmlSanitizer(AgreementHtmlPolicy policy) {
        return new JsoupHtmlSanitizer(policy);
    }
}
