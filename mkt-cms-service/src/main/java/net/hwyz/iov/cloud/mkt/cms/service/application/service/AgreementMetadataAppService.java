package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.AgreementMetadataResult;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementScene;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementHtmlPolicy;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.SanitizedHtml;
import net.hwyz.iov.cloud.mkt.cms.service.domain.port.SupportedScopeProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 协议管理元数据应用服务。
 * <p>
 * 管理端下拉与提示全部由后端受支持清单驱动：新增语言/市场/品牌/车系只需调整配置，
 * 前端不需要硬编码，也不需要重新发布前端即可生效。
 *
 * @author hwyz_leo
 */
@Service
@RequiredArgsConstructor
public class AgreementMetadataAppService {

    private final SupportedScopeProvider supportedScopeProvider;

    private final AgreementHtmlPolicy agreementHtmlPolicy;

    /**
     * 查询管理端元数据
     *
     * @return 元数据
     */
    public AgreementMetadataResult metadata() {
        Map<String, List<String>> carLinesByBrand = new LinkedHashMap<>();
        supportedScopeProvider.carLinesByBrand()
                .forEach((brand, carLines) -> carLinesByBrand.put(brand, new ArrayList<>(carLines)));
        return AgreementMetadataResult.builder()
                .agreementTypes(Arrays.stream(AgreementType.values()).map(Enum::name).collect(Collectors.toList()))
                .scenes(Arrays.stream(AgreementScene.values()).map(Enum::name).collect(Collectors.toList()))
                .markets(sorted(supportedScopeProvider.supportedMarkets()))
                .locales(sorted(supportedScopeProvider.supportedLocales()))
                .brands(sorted(supportedScopeProvider.supportedBrands()))
                .carLines(sorted(supportedScopeProvider.supportedCarLines()))
                .carLinesByBrand(carLinesByBrand)
                .contentType(SanitizedHtml.CONTENT_TYPE)
                .sanitizerVersion(agreementHtmlPolicy.sanitizerVersion())
                .maxInputBytes(agreementHtmlPolicy.getMaxInputBytes())
                .maxOutputBytes(agreementHtmlPolicy.getMaxOutputBytes())
                .build();
    }

    private List<String> sorted(java.util.Set<String> values) {
        return values.stream().sorted().collect(Collectors.toList());
    }
}
