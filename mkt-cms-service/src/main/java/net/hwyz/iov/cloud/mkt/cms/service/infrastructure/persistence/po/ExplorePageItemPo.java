package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.time.Instant;

/**
 * 探索页页面条目持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_page_item")
public class ExplorePageItemPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("item_id")
    private String itemId;

    @TableField("component_id")
    private String componentId;

    @TableField("item_type")
    private String itemType;

    @TableField("ref_id")
    private String refId;

    @TableField("ref_revision_id")
    private String refRevisionId;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField(value = "start_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant startAt;

    @TableField(value = "end_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant endAt;

    @TableField("target_type")
    private String targetType;

    @TableField("target_ref_id")
    private String targetRefId;

    @TableField("target_revision_id")
    private String targetRevisionId;

    @TableField("target_url")
    private String targetUrl;

    @TableField("target_route_code")
    private String targetRouteCode;

    @TableField("target_params")
    private String targetParams;

    @TableField("display_snapshot")
    private String displaySnapshot;
}
