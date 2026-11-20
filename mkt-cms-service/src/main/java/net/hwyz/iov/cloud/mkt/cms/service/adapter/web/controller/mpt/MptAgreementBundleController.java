package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.AgreementWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.support.OperatorResolver;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.PublishAgreementBundleRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementBundleSummaryResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementCurrentBundleResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementPublishResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementScopeQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementSearchQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementPublishAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 协议发布包管理（MPT 管理后台）。
 * <p>
 * MKT-CMS-DSN-CR-001 §4.1/§5：人工立即发布完整协议包，要求 requestId 与 expectedBundleId。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/agreement/v1/agreement-bundles")
public class MptAgreementBundleController extends BaseController {

    private final AgreementPublishAppService agreementPublishAppService;
    private final AgreementContentAppService agreementContentAppService;

    /**
     * 分页检索历史发布包
     *
     * @param scene       场景
     * @param market      市场
     * @param locale      内容语言
     * @param brandCode   品牌
     * @param carLineCode 车系
     * @return 发布包分页列表
     */
    @RequiresPermissions("mkt:cms:agreement:query")
    @GetMapping("/list")
    public ApiResponse<PageResult<AgreementBundleSummaryResponse>> list(
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "locale", required = false) String locale,
            @RequestParam(value = "brandCode", required = false) String brandCode,
            @RequestParam(value = "carLineCode", required = false) String carLineCode) {
        startPage();
        List<AgreementBundleSummaryResponse> list = PageUtil.convert(agreementContentAppService
                .searchBundles(AgreementSearchQuery.builder()
                        .scene(scene)
                        .market(market)
                        .locale(locale)
                        .brandCode(brandCode)
                        .carLineCode(carLineCode)
                        .build()), AgreementWebAssembler::toVo);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 查看指定范围当前发布包（含两份正文只读预览，用于发布页回显 expectedBundleId）
     *
     * @param scene       场景（必传）
     * @param market      市场（省略补默认）
     * @param locale      内容语言（省略补默认）
     * @param brandCode   品牌（省略补默认）
     * @param carLineCode 车系（省略补默认）
     * @return 当前发布包
     */
    @RequiresPermissions("mkt:cms:agreement:query")
    @GetMapping("/current")
    public ApiResponse<AgreementCurrentBundleResponse> current(
            @RequestParam("scene") String scene,
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "locale", required = false) String locale,
            @RequestParam(value = "brandCode", required = false) String brandCode,
            @RequestParam(value = "carLineCode", required = false) String carLineCode) {
        return ApiResponse.ok(AgreementWebAssembler.toCurrentBundleVo(
                agreementContentAppService.getCurrentBundle(AgreementScopeQuery.builder()
                        .scene(scene)
                        .market(market)
                        .locale(locale)
                        .brandCode(brandCode)
                        .carLineCode(carLineCode)
                        .build())));
    }

    /**
     * 立即发布完整协议包（原子切换当前指针）
     *
     * @param request 发布请求
     * @return 发布包
     */
    @RequiresPermissions("mkt:cms:agreement:publish")
    @PostMapping("/action/publish")
    public ApiResponse<AgreementPublishResponse> publish(@RequestBody @Valid PublishAgreementBundleRequest request) {
        return ApiResponse.ok(AgreementWebAssembler.toVo(
                agreementPublishAppService.publish(
                        AgreementWebAssembler.toCmd(request, OperatorResolver.currentOperator()))));
    }
}
