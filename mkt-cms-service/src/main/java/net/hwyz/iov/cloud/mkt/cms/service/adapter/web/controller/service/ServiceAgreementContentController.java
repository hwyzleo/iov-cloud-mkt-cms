package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ServletUtil;
import net.hwyz.iov.cloud.mkt.cms.api.service.AgreementContentService;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementBundleResponse;
import net.hwyz.iov.cloud.mkt.cms.api.vo.response.AgreementContentResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.AgreementWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementScopeQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协议内容内部服务接口实现（受信下游调用）。
 * <p>
 * MKT-CMS-DSN-CR-001 §6：只服务 TSP 等受信服务身份，不直接接受匿名车端公网读取；
 * 当前包响应显式 Cache-Control: no-store，最小实现不启用应用级当前包缓存；
 * 读取不产生任何同意、绑定或激活副作用。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service/content/v1")
public class ServiceAgreementContentController implements AgreementContentService {

    private static final String NO_STORE = "no-store";

    private final AgreementContentAppService agreementContentAppService;

    @Override
    public AgreementBundleResponse getCurrentBundle(String scene, String market, String locale,
                                                    String brandCode, String carLineCode) {
        AgreementBundleResponse response = AgreementWebAssembler.toApiResponse(
                agreementContentAppService.getCurrentBundle(AgreementScopeQuery.builder()
                        .scene(scene)
                        .market(market)
                        .locale(locale)
                        .brandCode(brandCode)
                        .carLineCode(carLineCode)
                        .build()));
        // §6.1：最小实现不启用应用级当前包缓存，当前包响应显式 no-store
        ServletUtil.getResponse().setHeader("Cache-Control", NO_STORE);
        log.info("按范围读取当前协议发布包: scene={}, bundleId={}", scene, response.getBundleId());
        return response;
    }

    @Override
    public AgreementContentResponse getPublishedRevision(String revisionId) {
        return AgreementWebAssembler.toApiResponse(
                agreementContentAppService.getPublishedRevision(revisionId));
    }
}
