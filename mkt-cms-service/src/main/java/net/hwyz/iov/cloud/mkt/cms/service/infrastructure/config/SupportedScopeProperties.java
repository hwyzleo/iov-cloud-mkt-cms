package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 受支持范围清单配置。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1：首版只开放 zh-CN 与 DEFAULT 市场/品牌/车系，
 * 后续通过受支持清单与对应内容配置扩展，不重建历史协议身份；
 * 品牌 → 车系归属显式登记，不依赖层级回退。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cms.agreement.supported")
public class SupportedScopeProperties implements SupportedScopeProvider {

    /**
     * 受支持语言
     */
    private Set<String> locales = new LinkedHashSet<>(Collections.singletonList("zh-CN"));

    /**
     * 受支持市场
     */
    private Set<String> markets = new LinkedHashSet<>(Collections.singletonList("DEFAULT"));

    /**
     * 受支持品牌
     */
    private Set<String> brands = new LinkedHashSet<>(Collections.singletonList("DEFAULT"));

    /**
     * 受支持车系
     */
    private Set<String> carLines = new LinkedHashSet<>(Collections.singletonList("DEFAULT"));

    /**
     * 品牌 → 车系归属，形如 BRAND:CAR_LINE，逗号分隔
     */
    private String brandCarLines = "";

    @Override
    public Set<String> supportedMarkets() {
        return markets;
    }

    @Override
    public Set<String> supportedLocales() {
        return locales;
    }

    @Override
    public Set<String> supportedBrands() {
        return brands;
    }

    @Override
    public Set<String> supportedCarLines() {
        return carLines;
    }

    @Override
    public boolean isCarLineOwnedByBrand(String brandCode, String carLineCode) {
        return ownership().getOrDefault(brandCode, Collections.emptySet()).contains(carLineCode);
    }

    private Map<String, Set<String>> ownership() {
        Map<String, Set<String>> ownership = new LinkedHashMap<>();
        if (brandCarLines == null || brandCarLines.trim().isEmpty()) {
            return ownership;
        }
        for (String pair : brandCarLines.split(",")) {
            String[] parts = pair.split(":");
            if (parts.length != 2) {
                continue;
            }
            String brand = parts[0].trim();
            String carLine = parts[1].trim();
            if (brand.isEmpty() || carLine.isEmpty()) {
                continue;
            }
            ownership.computeIfAbsent(brand, key -> new LinkedHashSet<>()).add(carLine);
        }
        return ownership;
    }
}
