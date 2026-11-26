package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 探索页页面组件持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_page_component")
public class ExplorePageComponentPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("component_id")
    private String componentId;

    @TableField("page_id")
    private String pageId;

    @TableField("component_type")
    private String componentType;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("config_json")
    private String configJson;
}
