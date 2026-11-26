package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 探索页子域配置（白名单、富文本上限与调度参数）。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.3 / D-012：素材、H5 origin 与 NATIVE 路由均使用服务端白名单；
 * 白名单默认空表示“不允许任何外链”，由部署环境显式配置，避免安全默认被绕过。
 *
 * @author hwyz_leo
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cms.explore")
public class ExploreProperties {

    /**
     * 素材域名白名单（HTTPS host，可含端口）
     */
    private List<String> allowedAssetHosts = new ArrayList<>();

    /**
     * H5 origin 白名单（scheme://host[:port]）
     */
    private List<String> allowedH5Origins = new ArrayList<>();

    /**
     * NATIVE 路由编码白名单
     */
    private List<String> nativeRouteCodes = new ArrayList<>();

    /**
     * NATIVE 受控参数键白名单
     */
    private List<String> nativeParamKeys = new ArrayList<>();

    /**
     * 文章富文本输入上限（字节）
     */
    private int htmlMaxInputBytes = 1024 * 1024;

    /**
     * 文章富文本输出上限（字节）
     */
    private int htmlMaxOutputBytes = 1024 * 1024;

    /**
     * 是否启用数据库调度器（定时生效/到期）
     */
    private boolean schedulerEnabled = true;

    /**
     * 调度扫描间隔（毫秒）
     */
    private long schedulerIntervalMs = 30000L;

    /**
     * 单批扫描上限
     */
    private int schedulerBatchSize = 50;

    /**
     * 信息流默认页大小
     */
    private int feedPageSizeDefault = 10;

    /**
     * 信息流最大页大小
     */
    private int feedPageSizeMax = 20;
}
