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
import java.time.LocalDate;

/**
 * 协议修订表持久化对象（插入后不可改删）。
 *
 * @author hwyz_leo
 */
@Data
@TableName("cms_agreement_revision")
public class AgreementRevisionPo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("revision_id")
    private String revisionId;

    @TableField("agreement_code")
    private String agreementCode;

    @TableField("agreement_type")
    private String agreementType;

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

    @TableField("title")
    private String title;

    @TableField("version")
    private String version;

    @TableField("agreement_date")
    private LocalDate agreementDate;

    @TableField("content_type")
    private String contentType;

    @TableField("body")
    private String body;

    @TableField("content_hash")
    private String contentHash;

    @TableField("sanitizer_version")
    private String sanitizerVersion;

    @TableField("source_draft_id")
    private String sourceDraftId;

    @TableField("source_draft_version")
    private Integer sourceDraftVersion;

    @TableField("frozen_by")
    private String frozenBy;

    @TableField(value = "frozen_at", jdbcType = JdbcType.TIMESTAMP, typeHandler = UtcInstantTypeHandler.class)
    private Instant frozenAt;
}
