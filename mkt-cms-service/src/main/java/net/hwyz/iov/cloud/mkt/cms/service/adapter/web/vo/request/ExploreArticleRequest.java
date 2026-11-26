package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

/**
 * 探索页文章新建/修改请求。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreArticleRequest {

    @NotBlank(message = "文章来源类型不能为空")
    private String sourceType;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String summary;

    private ExploreImageRequest cover;

    /**
     * 多图列表（最多 9 张）；优先使用，为空时兼容单封面 cover
     */
    private java.util.List<ExploreImageRequest> images;

    private String authorName;

    private String authorAvatarUrl;

    private Instant displayAt;

    /**
     * 官方文章安全 HTML（UGC 引用必须为空）
     */
    private String body;

    /**
     * UGC 来源内容标识
     */
    private String sourceContentId;

    /**
     * UGC 原生路由编码
     */
    private String nativeRouteCode;

    /**
     * 修改时必填
     */
    private Integer lockVersion;
}
