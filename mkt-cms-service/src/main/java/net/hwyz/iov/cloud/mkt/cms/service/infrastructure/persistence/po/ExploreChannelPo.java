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
 * 探索页频道持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_channel")
public class ExploreChannelPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("channel_code")
    private String channelCode;

    @TableField("name")
    private String name;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("status")
    private String status;

    @TableField("lock_version")
    private Integer lockVersion;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "create_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant createTime;

    @TableField("modify_by")
    private String modifyBy;

    @TableField(value = "modify_time", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant modifyTime;
}
