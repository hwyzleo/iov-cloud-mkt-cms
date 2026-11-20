package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理侧当前协议发布包（含两份协议内容，供发布回显与只读预览）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class AgreementCurrentBundleResponse {

    private String bundleId;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private Instant publishedAt;

    private List<AgreementItem> agreements;

    /**
     * 单份协议内容
     */
    @Data
    @Builder
    public static class AgreementItem {

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
