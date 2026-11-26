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
 * 探索页频道页面版本持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_page")
public class ExplorePagePo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("page_id")
    private String pageId;

    @TableField("channel_code")
    private String channelCode;

    @TableField("page_version")
    private Integer pageVersion;

    @TableField("status")
    private String status;

    @TableField(value = "effective_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant effectiveAt;

    @TableField(value = "expire_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant expireAt;

    @TableField("request_id")
    private String requestId;

    @TableField("request_hash")
    private String requestHash;

    @TableField("expected_release_id")
    private String expectedReleaseId;

    @TableField("lock_version")
    private Integer lockVersion;

    @TableField("published_by")
    private String publishedBy;

    @TableField(value = "published_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant publishedAt;

    @TableField(value = "offline_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant offlineAt;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "create_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant createTime;

    @TableField("modify_by")
    private String modifyBy;

    @TableField(value = "modify_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant modifyTime;
}
