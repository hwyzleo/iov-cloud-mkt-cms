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
 * 协议操作留痕表持久化对象。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_agreement_operation")
public class AgreementOperationPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("operation_id")
    private String operationId;

    @TableField("action")
    private String action;

    @TableField("object_type")
    private String objectType;

    @TableField("object_id")
    private String objectId;

    @TableField("operator")
    private String operator;

    @TableField(value = "operated_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant operatedAt;

    @TableField("result")
    private String result;

    @TableField("trace_id")
    private String traceId;
}
