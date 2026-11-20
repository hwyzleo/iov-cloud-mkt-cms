package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 协议草稿出参。
 * <p>
 * 返回清洗后的实际正文、清洗策略版本与是否发生调整，供维护人员核对保存结果。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementDraftResult {

    private String draftId;

    private String agreementType;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private String title;

    private String version;

    private LocalDate agreementDate;

    private String contentType;

    private String body;

    private String sanitizerVersion;

    /**
     * 本次保存是否相对请求原文发生了清洗调整
     */
    private boolean adjusted;

    private int lockVersion;

    private String updatedBy;

    private Instant updatedAt;
}
