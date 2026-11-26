package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 探索页素材上传申请请求。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreAssetUploadRequest {

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不得超过 255")
    private String originalFilename;

    @NotBlank(message = "图片 MIME 不能为空")
    private String contentType;

    @NotNull(message = "图片大小不能为空")
    @Min(value = 1, message = "图片大小必须大于 0")
    @Max(value = 10485760, message = "图片大小不得超过 10MB")
    private Long size;
}
