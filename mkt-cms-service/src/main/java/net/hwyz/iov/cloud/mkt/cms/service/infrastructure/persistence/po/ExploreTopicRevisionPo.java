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
 * 探索页专题修订持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_topic_revision")
public class ExploreTopicRevisionPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("revision_id")
    private String revisionId;

    @TableField("topic_id")
    private String topicId;

    @TableField("revision_no")
    private Integer revisionNo;

    @TableField("status")
    private String status;

    @TableField("name")
    private String name;

    @TableField("summary")
    private String summary;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("cover_width")
    private Integer coverWidth;

    @TableField("cover_height")
    private Integer coverHeight;

    @TableField("cover_mime")
    private String coverMime;

    @TableField("cover_size")
    private Long coverSize;

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
