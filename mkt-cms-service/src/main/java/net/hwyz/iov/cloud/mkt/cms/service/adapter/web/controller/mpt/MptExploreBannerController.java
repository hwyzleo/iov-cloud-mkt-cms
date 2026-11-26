package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mpt;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.assembler.ExploreWebAssembler;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.support.OperatorResolver;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreBannerRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreContentPublishRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreBannerAppService;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreContentSearchCriteria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 探索页 Banner 管理。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1/banners")
public class MptExploreBannerController extends BaseController {

    private final ExploreBannerAppService bannerAppService;

    /**
     * Banner 物理分页检索
     *
     * @param status  根实体状态
     * @param keyword 标题模糊
     * @return Banner 分页列表
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/list")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        startPage();
        List<Map<String, Object>> list = PageUtil.convert(bannerAppService.search(
                        ExploreContentSearchCriteria.builder()
                                .status(status)
                                .keyword(keyword)
                                .build()),
                ExploreWebAssembler::toBannerListView);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 新建 Banner 草稿
     *
     * @param request 新建请求
     * @return Banner 详情
     */
    @RequiresPermissions("mkt:cms:explore:add")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody @Valid ExploreBannerRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toBannerView(bannerAppService.create(
                ExploreWebAssembler.toCmd(null, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 查询 Banner 详情
     *
     * @param bannerId Banner 标识
     * @return Banner 详情
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/{bannerId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable("bannerId") String bannerId) {
        return ApiResponse.ok(ExploreWebAssembler.toBannerView(bannerAppService.get(bannerId)));
    }

    /**
     * 修改 Banner（携带 lockVersion）
     *
     * @param bannerId Banner 标识
     * @param request  修改请求
     * @return Banner 详情
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @PutMapping("/{bannerId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("bannerId") String bannerId,
                                                   @RequestBody @Valid ExploreBannerRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toBannerView(bannerAppService.update(
                ExploreWebAssembler.toCmd(bannerId, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 发布指定草稿修订
     *
     * @param bannerId Banner 标识
     * @param request  发布请求
     * @return Banner 详情
     */
    @RequiresPermissions("mkt:cms:explore:publish")
    @PostMapping("/{bannerId}/action/publish")
    public ApiResponse<Map<String, Object>> publish(
            @PathVariable("bannerId") String bannerId,
            @RequestBody(required = false) ExploreContentPublishRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toBannerView(bannerAppService.publish(
                ExploreWebAssembler.toContentPublishCmd(bannerId, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 立即下线 Banner
     *
     * @param bannerId Banner 标识
     * @return Banner 详情
     */
    @RequiresPermissions("mkt:cms:explore:offline")
    @PostMapping("/{bannerId}/action/offline")
    public ApiResponse<Map<String, Object>> offline(@PathVariable("bannerId") String bannerId) {
        return ApiResponse.ok(ExploreWebAssembler.toBannerView(bannerAppService.offline(
                ExploreWebAssembler.toContentPublishCmd(bannerId, null, OperatorResolver.currentOperator()))));
    }
}
