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
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreContentPublishRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreTopicRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreTopicAppService;
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
 * 探索页专题管理（有序文章条目与四类跳转目标）。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1/topics")
public class MptExploreTopicController extends BaseController {

    private final ExploreTopicAppService topicAppService;

    /**
     * 专题物理分页检索
     *
     * @param status  根实体状态
     * @param keyword 名称模糊
     * @return 专题分页列表
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/list")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        startPage();
        List<Map<String, Object>> list = PageUtil.convert(topicAppService.search(
                        ExploreContentSearchCriteria.builder()
                                .status(status)
                                .keyword(keyword)
                                .build()),
                ExploreWebAssembler::toTopicListView);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 新建专题草稿
     *
     * @param request 新建请求
     * @return 专题详情
     */
    @RequiresPermissions("mkt:cms:explore:add")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody @Valid ExploreTopicRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toTopicView(topicAppService.create(
                ExploreWebAssembler.toCmd(null, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 查询专题详情
     *
     * @param topicId 专题标识
     * @return 专题详情
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/{topicId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable("topicId") String topicId) {
        return ApiResponse.ok(ExploreWebAssembler.toTopicView(topicAppService.get(topicId)));
    }

    /**
     * 修改专题（携带 lockVersion）
     *
     * @param topicId 专题标识
     * @param request 修改请求
     * @return 专题详情
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @PutMapping("/{topicId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("topicId") String topicId,
                                                   @RequestBody @Valid ExploreTopicRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toTopicView(topicAppService.update(
                ExploreWebAssembler.toCmd(topicId, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 发布指定草稿修订
     *
     * @param topicId 专题标识
     * @param request 发布请求
     * @return 专题详情
     */
    @RequiresPermissions("mkt:cms:explore:publish")
    @PostMapping("/{topicId}/action/publish")
    public ApiResponse<Map<String, Object>> publish(
            @PathVariable("topicId") String topicId,
            @RequestBody(required = false) ExploreContentPublishRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toTopicView(topicAppService.publish(
                ExploreWebAssembler.toContentPublishCmd(topicId, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 立即下线专题
     *
     * @param topicId 专题标识
     * @return 专题详情
     */
    @RequiresPermissions("mkt:cms:explore:offline")
    @PostMapping("/{topicId}/action/offline")
    public ApiResponse<Map<String, Object>> offline(@PathVariable("topicId") String topicId) {
        return ApiResponse.ok(ExploreWebAssembler.toTopicView(topicAppService.offline(
                ExploreWebAssembler.toContentPublishCmd(topicId, null, OperatorResolver.currentOperator()))));
    }
}
