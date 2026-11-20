package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 协议草稿列表项（不含正文，避免列表大 payload）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementDraftSummaryResult {

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

    private String sanitizerVersion;

    private int lockVersion;

    private String updatedBy;

    private Instant updatedAt;
}
