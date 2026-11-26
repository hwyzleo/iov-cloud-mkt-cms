package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.ExploreHtmlSanitizer;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreCursorCodec;
import net.hwyz.iov.cloud.mkt.cms.service.domain.service.ExploreMediaValidator;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.sanitize.ExploreJsoupHtmlSanitizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.HashSet;

/**
 * 探索页子域域层对象装配。
 * <p>
 * 域层不依赖 Spring：清洗策略、白名单校验器与游标编解码器由本配置显式装配为 Bean；
 * 统一注入 UTC Clock，便于集成测试使用可控时钟验证定时生效/到期。
 *
 * @author hwyz_leo
 */
@Configuration
public class ExploreDomainConfig {

    /**
     * 富文本清洗策略
     *
     * @param properties 探索页配置
     * @return 允许列表策略
     */
    @Bean
    public ExploreHtmlPolicy exploreHtmlPolicy(ExploreProperties properties) {
        return new ExploreHtmlPolicy(properties.getHtmlMaxInputBytes(), properties.getHtmlMaxOutputBytes());
    }

    /**
     * 素材、H5 与 NATIVE 白名单校验器
     *
     * @param properties 探索页配置
     * @return 校验器
     */
    @Bean
    public ExploreMediaValidator exploreMediaValidator(ExploreProperties properties) {
        return new ExploreMediaValidator(new HashSet<>(properties.getAllowedAssetHosts()),
                new HashSet<>(properties.getAllowedH5Origins()),
                new HashSet<>(properties.getNativeRouteCodes()),
                new HashSet<>(properties.getNativeParamKeys()));
    }

    /**
     * 官方文章富文本清洗器
     *
     * @param policy         清洗策略
     * @param mediaValidator 白名单校验器
     * @return 清洗端口实现
     */
    @Bean
    public ExploreHtmlSanitizer exploreHtmlSanitizer(ExploreHtmlPolicy policy,
                                                     ExploreMediaValidator mediaValidator) {
        return new ExploreJsoupHtmlSanitizer(policy, mediaValidator);
    }

    /**
     * keyset 游标编解码器
     *
     * @return 编解码器
     */
    @Bean
    public ExploreCursorCodec exploreCursorCodec() {
        return new ExploreCursorCodec();
    }

    /**
     * 业务时钟（全 UTC；集成测试可替换为固定时钟）
     *
     * @return UTC 时钟
     */
    @Bean
    public Clock exploreClock() {
        return Clock.systemUTC();
    }
}
