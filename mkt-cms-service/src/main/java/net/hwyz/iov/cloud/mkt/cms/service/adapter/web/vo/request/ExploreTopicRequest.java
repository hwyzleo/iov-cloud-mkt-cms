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

    private ExploreTargetRequest target;

    private List<String> articleRevisionIds;

    private Integer lockVersion;
}
