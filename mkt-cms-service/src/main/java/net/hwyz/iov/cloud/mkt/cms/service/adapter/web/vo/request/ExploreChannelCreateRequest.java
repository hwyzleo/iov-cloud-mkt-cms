package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 探索页新增频道请求（编码创建后不可变）。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreChannelCreateRequest {

    @NotBlank(message = "频道编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,31}$",
            message = "频道编码仅允许大写字母、数字和下划线，且必须以字母开头，长度不超过 32")
    private String channelCode;

    @NotBlank(message = "频道名称不能为空")
    private String name;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不得为负")
    private Integer sortNo;

    @NotBlank(message = "频道状态不能为空")
    private String status;
}
