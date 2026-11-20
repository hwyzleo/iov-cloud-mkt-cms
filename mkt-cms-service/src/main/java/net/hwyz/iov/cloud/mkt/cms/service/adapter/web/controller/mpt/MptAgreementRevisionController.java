package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.AgreementWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementRevisionResponse;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response.AgreementRevisionSummaryResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.query.AgreementSearchQuery;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.AgreementContentAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 协议修订管理（MPT 管理后台）。
 * <p>
 * 管理侧可查看未发布修订；本接口不暴露给普通业务读取方。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/agreement/v1/agreement-revisions")
public class MptAgreementRevisionController extends BaseController {

    private final AgreementContentAppService agreementContentAppService;

    /**
     * 分页检索修订（列表不含正文；published=true 时仅返回已通过发布包发布的修订）
     *
     * @param agreementType 协议类型
     * @param scene         场景
     * @param market        市场
     * @param locale        内容语言
     * @param brandCode     品牌
     * @param carLineCode   车系
     * @param title         标题（模糊）
     * @param version       业务版本（精确）
     * @param published     是否仅查已发布修订
     * @return 修订分页列表
     */
    @RequiresPermissions("mkt:cms:agreement:query")
    @GetMapping("/list")
    public ApiResponse<PageResult<AgreementRevisionSummaryResponse>> list(
            @RequestParam(value = "agreementType", required = false) String agreementType,
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "locale", required = false) String locale,
            @RequestParam(value = "brandCode", required = false) String brandCode,
            @RequestParam(value = "carLineCode", required = false) String carLineCode,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "published", required = false) Boolean published) {
        startPage();
        List<AgreementRevisionSummaryResponse> list = PageUtil.convert(agreementContentAppService
                .searchRevisions(AgreementSearchQuery.builder()
                        .agreementType(agreementType)
                        .scene(scene)
                        .market(market)
                        .locale(locale)
                        .brandCode(brandCode)
                        .carLineCode(carLineCode)
                        .title(title)
                        .version(version)
                        .published(published)
                        .build()), AgreementWebAssembler::toVo);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 查询修订
     *
     * @param revisionId 修订标识
     * @return 修订内容
     */
    @RequiresPermissions("mkt:cms:agreement:query")
    @GetMapping("/{revisionId}")
    public ApiResponse<AgreementRevisionResponse> get(@PathVariable("revisionId") String revisionId) {
        return ApiResponse.ok(AgreementWebAssembler.toVo(
                agreementContentAppService.getRevisionForManagement(revisionId)));
    }
}
