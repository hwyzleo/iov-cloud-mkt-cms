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
 * 协议发布包表持久化对象（不可变）。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_agreement_bundle")
public class AgreementBundlePo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("bundle_id")
    private String bundleId;

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

    @TableField("privacy_revision_id")
    private String privacyRevisionId;

    @TableField("user_revision_id")
    private String userRevisionId;

    @TableField("published_by")
    private String publishedBy;

    @TableField(value = "published_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant publishedAt;

    @TableField("request_id")
    private String requestId;

    @TableField("request_hash")
    private String requestHash;

    @TableField("expected_bundle_id")
    private String expectedBundleId;
}
