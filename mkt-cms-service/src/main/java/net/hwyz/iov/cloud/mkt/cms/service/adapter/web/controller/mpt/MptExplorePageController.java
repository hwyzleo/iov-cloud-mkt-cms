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
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExplorePagePublishRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExplorePageSaveRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.dto.result.ExplorePageData;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExplorePageAppService;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExplorePublishAppService;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageSearchCriteria;
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
 * 探索页频道页面编排、预览、发布与下线管理。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1")
public class MptExplorePageController extends BaseController {

    private final ExplorePageAppService pageAppService;
    private final ExplorePublishAppService publishAppService;

    /**
     * 读取频道草稿
     *
     * @param channelCode 频道编码
     * @return 草稿视图
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/pages/{channelCode}")
    public ApiResponse<Map<String, Object>> getDraft(@PathVariable("channelCode") String channelCode) {
        return ApiResponse.ok(ExploreWebAssembler.toDraftView(pageAppService.getDraft(channelCode)));
    }

    /**
     * 整体保存频道草稿（必须携带 lockVersion）
     *
     * @param channelCode 频道编码
     * @param request     保存请求
     * @return 保存后的草稿
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @PutMapping("/pages/{channelCode}")
    public ApiResponse<Map<String, Object>> save(@PathVariable("channelCode") String channelCode,
                                                 @RequestBody @Valid ExplorePageSaveRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toDraftView(pageAppService.save(
                ExploreWebAssembler.toCmd(channelCode, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 读取频道当前生效版本（发布弹窗展示 expectedReleaseId）
     *
     * @param channelCode 频道编码
     * @return 当前版本；不存在时 releaseId 为空
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/pages/{channelCode}/current")
    public ApiResponse<Map<String, Object>> current(@PathVariable("channelCode") String channelCode) {
        return ApiResponse.ok(ExploreWebAssembler.toCurrentReleaseView(
                pageAppService.getCurrentPage(channelCode).getPage()));
    }

    /**
     * 立即或定时发布页面
     *
     * @param channelCode 频道编码
     * @param request     发布请求
     * @return 发布版本
     */
    @RequiresPermissions("mkt:cms:explore:publish")
    @PostMapping("/pages/{channelCode}/action/publish")
    public ApiResponse<Map<String, Object>> publish(@PathVariable("channelCode") String channelCode,
                                                    @RequestBody @Valid ExplorePagePublishRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toPublishView(publishAppService.publish(
                ExploreWebAssembler.toCmd(channelCode, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 取消待生效版本（不改变当前页面）
     *
     * @param channelCode 频道编码
     * @return 取消结果
     */
    @RequiresPermissions("mkt:cms:explore:publish")
    @PostMapping("/pages/{channelCode}/action/cancel-schedule")
    public ApiResponse<Map<String, Object>> cancelSchedule(@PathVariable("channelCode") String channelCode) {
        ExplorePage canceled = publishAppService.cancelSchedule(channelCode, OperatorResolver.currentOperator());
        return ApiResponse.ok(ExploreWebAssembler.toReleaseView(canceled));
    }

    /**
     * 立即下线频道页面（同时取消待生效版本）
     *
     * @param channelCode 频道编码
     * @return 下线结果
     */
    @RequiresPermissions("mkt:cms:explore:offline")
    @PostMapping("/pages/{channelCode}/action/offline")
    public ApiResponse<Map<String, Object>> offline(@PathVariable("channelCode") String channelCode) {
        ExplorePage offline = publishAppService.offline(channelCode, OperatorResolver.currentOperator());
        if (offline == null) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(ExploreWebAssembler.toReleaseView(offline));
    }

    /**
     * 发布历史物理分页检索
     *
     * @param channelCode 频道编码
     * @param status      发布状态
     * @return 发布版本分页列表
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/pages/{channelCode}/releases/list")
    public ApiResponse<PageResult<Map<String, Object>>> releases(
            @PathVariable("channelCode") String channelCode,
            @RequestParam(value = "status", required = false) String status) {
        startPage();
        List<Map<String, Object>> list = PageUtil.convert(pageAppService.searchReleases(
                        ExplorePageSearchCriteria.builder()
                                .channelCode(channelCode)
                                .status(status)
                                .build()),
                ExploreWebAssembler::toReleaseView);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 发布版本快照详情
     *
     * @param releaseId 发布版本标识
     * @return 快照详情
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/page-releases/{releaseId}")
    public ApiResponse<Map<String, Object>> releaseDetail(@PathVariable("releaseId") String releaseId) {
        return ApiResponse.ok(ExploreWebAssembler.toReleaseDetailView(
                pageAppService.releaseDetail(releaseId)));
    }

    /**
     * 历史版本复制为新草稿（不修改历史）
     *
     * @param releaseId 发布版本标识
     * @return 草稿视图
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @PostMapping("/page-releases/{releaseId}/action/copy-to-draft")
    public ApiResponse<Map<String, Object>> copyToDraft(@PathVariable("releaseId") String releaseId) {
        return ApiResponse.ok(ExploreWebAssembler.toDraftView(
                pageAppService.copyToDraft(releaseId, OperatorResolver.currentOperator())));
    }

    /**
     * 管理侧手机预览（读取草稿，不影响线上）
     *
     * @param pageId 草稿页面标识
     * @return 与手机契约同构的预览视图
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/page-drafts/{pageId}/preview")
    public ApiResponse<Map<String, Object>> preview(@PathVariable("pageId") String pageId) {
        return ApiResponse.ok(pageAppService.preview(pageId).toView());
    }
}
