package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 探索页 Banner 新建/修改请求。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreBannerRequest {

    @NotBlank(message = "Banner 标题不能为空")
    private String title;

    @NotNull(message = "Banner 主图不能为空")
    private ExploreImageRequest image;

    @NotNull(message = "Banner 跳转目标不能为空")
    private ExploreTargetRequest target;

    private Integer defaultSortNo;

    private Integer lockVersion;
}
