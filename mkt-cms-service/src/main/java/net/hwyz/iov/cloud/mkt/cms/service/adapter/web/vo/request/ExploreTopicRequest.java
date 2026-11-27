package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 探索页专题新建/修改请求（articleRevisionIds 顺序即人工顺序）。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreTopicRequest {

    @NotBlank(message = "专题名称不能为空")
    private String name;

    private String summary;

    private ExploreImageRequest cover;

    /**
     * 有序图集（最多 9 张；首张为封面）。与文章图集字段一致。
     */
    private List<ExploreImageRequest> images;

    private ExploreTargetRequest target;

    private List<String> articleRevisionIds;

    private Integer lockVersion;
}
