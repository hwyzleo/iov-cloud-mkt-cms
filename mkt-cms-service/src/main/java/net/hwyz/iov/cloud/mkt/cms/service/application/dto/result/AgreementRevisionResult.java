package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 协议修订出参（管理侧可查看未发布修订）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementRevisionResult {

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

    /**
     * 形如 sha256:&lt;64 位十六进制&gt;
     */
    private String contentHash;

    private String sanitizerVersion;

    private String sourceDraftId;

    private int sourceDraftVersion;

    private String frozenBy;

    private Instant frozenAt;
}
