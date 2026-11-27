package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 探索页素材请求（复用平台上传，CMS 只保存 URL 与元数据）。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreImageRequest {

    @NotBlank(message = "图片 URL 不能为空")
    private String url;

    @NotNull(message = "图片宽度不能为空")
    private Integer width;

    @NotNull(message = "图片高度不能为空")
    private Integer height;

    @NotBlank(message = "图片 MIME 不能为空")
    private String mime;

    @NotNull(message = "图片大小不能为空")
    private Long size;

    /**
     * 是否进入详情图集；仅专题图片使用，缺省 true
     */
    private Boolean showInDetail;
}
