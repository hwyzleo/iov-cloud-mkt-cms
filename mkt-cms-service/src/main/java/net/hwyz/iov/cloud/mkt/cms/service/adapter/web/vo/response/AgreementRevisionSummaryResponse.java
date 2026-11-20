package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 协议修订列表项（管理后台，不含正文）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class AgreementRevisionSummaryResponse {

    private String revisionId;

    private String agreementCode;

    private String agreementType;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private String title;

    private String version;

    private LocalDate agreementDate;

    private String contentHash;

    private String sanitizerVersion;

    private int sourceDraftVersion;

    private String frozenBy;

    private Instant frozenAt;
}
