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
 * 探索页 Banner 修订持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_banner_revision")
public class ExploreBannerRevisionPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("revision_id")
    private String revisionId;

    @TableField("banner_id")
    private String bannerId;

    @TableField("revision_no")
    private Integer revisionNo;

    @TableField("status")
    private String status;

    @TableField("title")
    private String title;

    @TableField("image_url")
    private String imageUrl;

    @TableField("image_width")
    private Integer imageWidth;

    @TableField("image_height")
    private Integer imageHeight;

    @TableField("image_mime")
    private String imageMime;

    @TableField("image_size")
    private Long imageSize;

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

    @TableField("default_sort_no")
    private Integer defaultSortNo;

    @TableField("content_hash")
    private String contentHash;

    @TableField(value = "published_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant publishedAt;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "create_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant createTime;

    @TableField("modify_by")
    private String modifyBy;

    @TableField(value = "modify_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant modifyTime;
}
