package net.hwyz.iov.cloud.mkt.cms.service.domain.service;

import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;

import java.util.Set;

/**
 * 唯一的协议范围解析器。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1：除场景必传外，其余四维省略时补默认值；
 * 显式 null 与空串在入口即被拒绝（本解析器只能收到"省略"或"具体值"）；
 * 显式不受支持值拒绝；品牌为 DEFAULT 时车系只能 DEFAULT，具体车系必须归属具体品牌；
 * 首版不做层级匹配，解析结果显式返回，管理、发布、读取与幂等哈希共用同一规范化范围。
 *
 * @author hwyz_leo
 */
public class ScopeResolver {

    private final SupportedScopeProvider supportedScopeProvider;

    public ScopeResolver(SupportedScopeProvider supportedScopeProvider) {
        this.supportedScopeProvider = supportedScopeProvider;
    }

    /**
     * 解析并规范化适用范围
     *
     * @param scene       场景（必传）
     * @param market      市场（省略补 DEFAULT）
     * @param locale      语言（省略补 zh-CN）
     * @param brandCode   品牌（省略补 DEFAULT）
     * @param carLineCode 车系（省略补 DEFAULT）
     * @return 规范化后的适用范围
     */
    public AgreementScope resolve(String scene, String market, String locale,
                                  String brandCode, String carLineCode) {
        AgreementScene resolvedScene = AgreementScene.parse(scene);
        String resolvedMarket = resolveDimension(market, AgreementScope.DEFAULT_MARKET,
                "市场", supportedScopeProvider.supportedMarkets());
        String resolvedLocale = resolveDimension(locale, AgreementScope.DEFAULT_LOCALE,
                "语言", supportedScopeProvider.supportedLocales());
        String resolvedBrand = resolveDimension(brandCode, AgreementScope.DEFAULT_BRAND,
                "品牌", supportedScopeProvider.supportedBrands());
        String resolvedCarLine = resolveDimension(carLineCode, AgreementScope.DEFAULT_CAR_LINE,
                "车系", supportedScopeProvider.supportedCarLines());
        validateBrandCarLine(resolvedBrand, resolvedCarLine);
        return AgreementScope.of(resolvedScene, resolvedMarket, resolvedLocale, resolvedBrand, resolvedCarLine);
    }

    private String resolveDimension(String value, String defaultValue, String label, Set<String> supported) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    label + "不允许显式空值");
        }
        if (!supported.contains(trimmed)) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "不受支持的" + label + ": " + value);
        }
        return trimmed;
    }

    private void validateBrandCarLine(String brandCode, String carLineCode) {
        if (AgreementScope.DEFAULT_CAR_LINE.equals(carLineCode)) {
            // 车系未细分：品牌级协议（含品牌 DEFAULT）合法
            return;
        }
        if (AgreementScope.DEFAULT_BRAND.equals(brandCode)) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "品牌为 DEFAULT 时车系只能为 DEFAULT");
        }
        if (!supportedScopeProvider.isCarLineOwnedByBrand(brandCode, carLineCode)) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "车系[" + carLineCode + "]不属于品牌[" + brandCode + "]");
        }
    }
}
