package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 当前协议发布包读取结果（含两份正文）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementBundleContentResult {

    private String bundleId;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private Instant publishedAt;

    /**
     * 固定两项协议，调用方按类型识别，不依赖顺序
     */
    private List<AgreementContentResult> agreements;

    /**
     * 单份协议内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgreementContentResult {

        private String agreementCode;

        private String agreementType;

        private String revisionId;

        private String version;

        private LocalDate agreementDate;

        private String title;

        private String scene;

        private String market;

        private String locale;

        private String brandCode;

        private String carLineCode;

        private String contentType;

        private String body;

        private String contentHash;

        private String sanitizerVersion;
    }
}
