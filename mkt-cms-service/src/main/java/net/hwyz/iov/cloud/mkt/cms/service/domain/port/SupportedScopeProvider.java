package net.hwyz.iov.cloud.mkt.cms.service.domain.port;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 受支持范围清单端口（首版只开放中文与 DEFAULT 市场/品牌/车系）。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1：后续启用其他语言、市场、品牌或车系通过受支持清单扩展，
 * 具体车系必须对应具体品牌且归属合法；品牌为 DEFAULT 时车系只能为 DEFAULT。
 *
 * @author hwyz_leo
 */
public interface SupportedScopeProvider {

    /**
     * 受支持的市场
     *
     * @return 市场清单
     */
    Set<String> supportedMarkets();

    /**
     * 受支持的语言
     *
     * @return 语言清单
     */
    Set<String> supportedLocales();

    /**
     * 受支持的品牌
     *
     * @return 品牌清单
     */
    Set<String> supportedBrands();

    /**
     * 受支持的车系
     *
     * @return 车系清单
     */
    Set<String> supportedCarLines();

    /**
     * 车系是否归属于指定品牌
     *
     * @param brandCode   品牌
     * @param carLineCode 车系
     * @return 归属合法返回 true
     */
    boolean isCarLineOwnedByBrand(String brandCode, String carLineCode);

    /**
     * 品牌 → 车系归属清单（供管理端渲染级联选择，扩展品牌/车系无需改前端）
     *
     * @return 品牌到车系列表的映射；未配置时为空映射
     */
    default Map<String, List<String>> carLinesByBrand() {
        return Collections.emptyMap();
    }
}
