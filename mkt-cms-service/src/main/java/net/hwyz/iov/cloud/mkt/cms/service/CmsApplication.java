package net.hwyz.iov.cloud.mkt.cms.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.annotation.EnableCustomConfig;
import net.hwyz.iov.cloud.framework.security.annotation.EnableCustomFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * MKT-CMS 内容管理服务启动类。
 *
 * @author hwyz_leo
 */
@Slf4j
@EnableCustomConfig
@EnableDiscoveryClient
@EnableCustomFeignClients
@SpringBootApplication
public class CmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmsApplication.class, args);
        log.info("应用启动完成");
    }
}
