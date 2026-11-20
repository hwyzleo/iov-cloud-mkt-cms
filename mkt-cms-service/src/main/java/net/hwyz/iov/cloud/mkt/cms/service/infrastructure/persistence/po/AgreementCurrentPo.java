package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.time.Instant;

/**
 * 协议当前发布指针表持久化对象（五维 Scope 为主键）。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_agreement_current")
public class AgreementCurrentPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("scene")
    private String scene;

    @TableField("market")
    private String market;

    @TableField("locale")
    private String locale;

    @TableField("brand_code")
    private String brandCode;

    @TableField("car_line_code")
    private String carLineCode;

    @TableField("bundle_id")
    private String bundleId;

    @TableField("lock_version")
    private Integer lockVersion;

    @TableField(value = "updated_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant updatedAt;
}
