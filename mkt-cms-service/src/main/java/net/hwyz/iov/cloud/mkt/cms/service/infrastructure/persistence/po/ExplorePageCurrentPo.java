package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.time.Instant;

/**
 * 探索页频道当前版本指针持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_explore_page_current")
public class ExplorePageCurrentPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("channel_code")
    private String channelCode;

    @TableField("release_id")
    private String releaseId;

    @TableField("lock_version")
    private Integer lockVersion;

    @TableField(value = "updated_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant updatedAt;
}
