package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 探索页跳转目标请求（判别联合）。
 *
 * @author hwyz_leo
 */
@Data
public class ExploreTargetRequest {

    /**
     * ARTICLE / TOPIC / H5 / NATIVE
     */
    @NotBlank(message = "跳转类型不能为空")
    private String type;

    /**
     * ARTICLE 目标文章标识
     */
    private String articleId;

    /**
     * TOPIC 目标专题标识
     */
    private String topicId;

    /**
     * ARTICLE / TOPIC 目标修订标识
     */
    private String revisionId;

    /**
     * H5 目标 URL
     */
    private String url;

    /**
     * NATIVE 路由编码
     */
    private String routeCode;

    /**
     * NATIVE 受控参数
     */
    private Map<String, String> params;
}
