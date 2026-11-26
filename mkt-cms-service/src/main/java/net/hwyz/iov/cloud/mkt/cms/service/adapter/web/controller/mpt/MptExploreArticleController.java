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
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreArticleRequest;
import net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request.ExploreContentPublishRequest;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreArticleAppService;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleSearchCriteria;
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
 * 探索页文章管理（官方富文本与 UGC 引用）。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mpt/explore/v1/articles")
public class MptExploreArticleController extends BaseController {

    private final ExploreArticleAppService articleAppService;

    /**
     * 文章物理分页检索（列表不含正文）
     *
     * @param sourceType 来源类型
     * @param status     根实体状态
     * @param title      标题模糊
     * @return 文章分页列表
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/list")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "title", required = false) String title) {
        startPage();
        List<Map<String, Object>> list = PageUtil.convert(articleAppService.search(
                        ExploreArticleSearchCriteria.builder()
                                .sourceType(sourceType)
                                .status(status)
                                .title(title)
                                .build()),
                ExploreWebAssembler::toArticleListView);
        return ApiResponse.ok(getPageResult(list));
    }

    /**
     * 新建文章草稿
     *
     * @param request 新建请求
     * @return 文章详情
     */
    @RequiresPermissions("mkt:cms:explore:add")
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody @Valid ExploreArticleRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toArticleView(articleAppService.create(
                ExploreWebAssembler.toCmd(null, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 查询文章详情（含最新草稿正文）
     *
     * @param articleId 文章标识
     * @return 文章详情
     */
    @RequiresPermissions("mkt:cms:explore:query")
    @GetMapping("/{articleId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable("articleId") String articleId) {
        return ApiResponse.ok(ExploreWebAssembler.toArticleView(articleAppService.get(articleId)));
    }

    /**
     * 修改文章（携带 lockVersion）
     *
     * @param articleId 文章标识
     * @param request   修改请求
     * @return 文章详情
     */
    @RequiresPermissions("mkt:cms:explore:edit")
    @PutMapping("/{articleId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable("articleId") String articleId,
                                                   @RequestBody @Valid ExploreArticleRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toArticleView(articleAppService.update(
                ExploreWebAssembler.toCmd(articleId, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 发布指定草稿修订（不传 revisionId 时发布最新草稿）
     *
     * @param articleId 文章标识
     * @param request   发布请求
     * @return 文章详情
     */
    @RequiresPermissions("mkt:cms:explore:publish")
    @PostMapping("/{articleId}/action/publish")
    public ApiResponse<Map<String, Object>> publish(
            @PathVariable("articleId") String articleId,
            @RequestBody(required = false) ExploreContentPublishRequest request) {
        return ApiResponse.ok(ExploreWebAssembler.toArticleView(articleAppService.publish(
                ExploreWebAssembler.toContentPublishCmd(articleId, request, OperatorResolver.currentOperator()))));
    }

    /**
     * 立即下线文章
     *
     * @param articleId 文章标识
     * @return 文章详情
     */
    @RequiresPermissions("mkt:cms:explore:offline")
    @PostMapping("/{articleId}/action/offline")
    public ApiResponse<Map<String, Object>> offline(@PathVariable("articleId") String articleId) {
        return ApiResponse.ok(ExploreWebAssembler.toArticleView(articleAppService.offline(
                ExploreWebAssembler.toContentPublishCmd(articleId, null, OperatorResolver.currentOperator()))));
    }
}
