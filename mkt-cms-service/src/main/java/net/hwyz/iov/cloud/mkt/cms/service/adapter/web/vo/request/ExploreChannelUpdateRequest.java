package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 探索页频道维护请求。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreChannelUpdateRequest {

    @NotBlank(message = "频道名称不能为空")
    private String name;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不得为负")
    private Integer sortNo;

    @NotBlank(message = "频道状态不能为空")
    private String status;

    @NotNull(message = "lockVersion 不能为空")
    private Integer lockVersion;
}
