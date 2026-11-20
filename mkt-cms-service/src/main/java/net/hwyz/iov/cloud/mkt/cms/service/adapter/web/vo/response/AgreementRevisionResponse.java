package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 协议修订出参（管理后台）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class AgreementRevisionResponse {

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

    private String contentType;

    private String body;

    private String contentHash;

    private String sanitizerVersion;

    private String sourceDraftId;

    private int sourceDraftVersion;

    private String frozenBy;

    private Instant frozenAt;
}
