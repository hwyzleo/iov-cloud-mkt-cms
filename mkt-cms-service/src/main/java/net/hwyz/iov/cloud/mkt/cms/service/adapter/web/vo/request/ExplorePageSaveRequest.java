package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 探索页频道草稿整体保存请求。
 *
 * @author hwyz_leo
 */
@Data
public class ExplorePageSaveRequest {

    @NotNull(message = "lockVersion 不能为空")
    private Integer lockVersion;

    private List<Component> components;

    /**
     * 草稿组件
     */
    @Data
    public static class Component {

        @NotBlank(message = "组件类型不能为空")
        private String type;

        @NotNull(message = "组件排序不能为空")
        @Min(value = 0, message = "组件排序不得为负")
        private Integer sortNo;

        private Map<String, Object> config;

        private List<Item> items;
    }

    /**
     * 草稿条目
     */
    @Data
    public static class Item {

        @NotBlank(message = "条目类型不能为空")
        private String itemType;

        @NotBlank(message = "引用标识不能为空")
        private String refId;

        @NotBlank(message = "引用修订标识不能为空")
        private String refRevisionId;

        @NotNull(message = "条目排序不能为空")
        @Min(value = 0, message = "条目排序不得为负")
        private Integer sortNo;

        private Instant startAt;

        private Instant endAt;

        private ExploreTargetRequest target;
    }
}
