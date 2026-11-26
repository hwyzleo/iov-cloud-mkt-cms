package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config;

import net.hwyz.iov.cloud.edd.oss.api.fallback.ImageServiceFallbackFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 探索页 EDD-OSS Feign 客户端降级工厂注册。
 * <p>
 * `edd-oss-api` 中的降级工厂位于本服务组件扫描范围之外，需显式注册；
 * 降级保持 fail-closed，不伪造上传地址或成功状态。
 *
 * @author hwyz_leo
 */
@Configuration
public class ExploreOssFeignConfig {

    /**
     * 图片上传客户端降级工厂
     *
     * @return 降级工厂
     */
    @Bean
    public ImageServiceFallbackFactory imageServiceFallbackFactory() {
        return new ImageServiceFallbackFactory();
    }
}
