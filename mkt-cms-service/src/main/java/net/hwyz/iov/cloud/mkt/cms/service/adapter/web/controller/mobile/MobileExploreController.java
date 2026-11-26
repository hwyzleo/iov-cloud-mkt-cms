package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.controller.mobile;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.mkt.cms.service.application.service.ExploreMobileQueryAppService;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 探索页手机端只读 API。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.2 / D-014：仅返回已启用频道与当前有效发布版本；
 * 页面响应使用 {@code Cache-Control: no-cache} 与 releaseId 派生 ETag，
 * 手机接口不接收用户画像、定位或推荐参数，不产生用户行为写入。
 *
 * @author hwyz_leo
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mobile/explore/v1")
public class MobileExploreController {

    private static final String NO_CACHE = "no-cache";

    private final ExploreMobileQueryAppService mobileQueryAppService;

    /**
     * 已启用且有当前有效页面的频道
     *
     * @return 频道列表
     */
    @GetMapping("/channels")
    public ApiResponse<List<Map<String, Object>>> channels() {
        return ApiResponse.ok(mobileQueryAppService.channels());
    }

    /**
     * 频道当前页面（含信息流首页）
     *
     * @param channelCode  频道编码
     * @param pageSize     信息流页大小（默认 10，最大 20）
     * @param ifNoneMatch  条件请求 ETag
     * @return 页面视图
     */
    @GetMapping("/pages/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> current(
            @RequestParam("channelCode") String channelCode,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        ExplorePageView page = mobileQueryAppService.currentPage(channelCode, pageSize);
        String etag = "\"" + page.getReleaseId() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("Cache-Control", NO_CACHE)
                    .eTag(page.getReleaseId())
                    .build();
        }
        return ResponseEntity.ok()
                .header("Cache-Control", NO_CACHE)
                .eTag(page.getReleaseId())
                .body(ApiResponse.ok(page.toView()));
    }

    /**
     * 文章信息流翻页（同一发布版本 keyset 物理分页）
     *
     * @param channelCode 频道编码
     * @param releaseId   首页返回的发布版本
     * @param cursor      不透明游标
     * @param pageSize    页大小
     * @return 信息流分页
     */
    @GetMapping("/article-feed")
    public ApiResponse<Map<String, Object>> articleFeed(
            @RequestParam("channelCode") String channelCode,
            @RequestParam("releaseId") String releaseId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(mobileQueryAppService.articleFeed(channelCode, releaseId, cursor, pageSize));
    }

    /**
     * 文章详情（仅已发布且未下线的指定修订）
     *
     * @param articleId  文章标识
     * @param revisionId 修订标识
     * @return 文章详情
     */
    @GetMapping("/articles/{articleId}/revisions/{revisionId}")
    public ApiResponse<Map<String, Object>> articleDetail(
            @PathVariable("articleId") String articleId,
            @PathVariable("revisionId") String revisionId) {
        return ApiResponse.ok(mobileQueryAppService.articleDetail(articleId, revisionId));
    }

    /**
     * 专题详情（指定已发布修订 + 稳定分页文章）
     *
     * @param topicId    专题标识
     * @param revisionId 修订标识
     * @param cursor     不透明游标
     * @param pageSize   页大小
     * @return 专题详情
     */
    @GetMapping("/topics/{topicId}/revisions/{revisionId}")
    public ApiResponse<Map<String, Object>> topicDetail(
            @PathVariable("topicId") String topicId,
            @PathVariable("revisionId") String revisionId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(mobileQueryAppService.topicDetail(topicId, revisionId, cursor, pageSize));
    }
}
