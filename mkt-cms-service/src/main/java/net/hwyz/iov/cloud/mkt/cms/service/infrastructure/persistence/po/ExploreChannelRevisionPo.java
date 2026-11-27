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
 * 探索页频道配置修订持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_channel_revision")
public class ExploreChannelRevisionPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("revision_id")
    private String revisionId;

    @TableField("channel_code")
    private String channelCode;

    @TableField("revision_no")
    private Integer revisionNo;

    @TableField("name")
    private String name;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("status")
    private String status;

    @TableField("revision_status")
    private String revisionStatus;

    @TableField("content_hash")
    private String contentHash;

    @TableField("published_by")
    private String publishedBy;

    @TableField(value = "published_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant publishedAt;

    @TableField("request_id")
    private String requestId;

    @TableField("request_hash")
    private String requestHash;

    @TableField("expected_current_revision_id")
    private String expectedCurrentRevisionId;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "create_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant createTime;

    @TableField("modify_by")
    private String modifyBy;

    @TableField(value = "modify_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant modifyTime;
}
