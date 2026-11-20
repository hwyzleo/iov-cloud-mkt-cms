package net.hwyz.iov.cloud.mkt.cms.service.adapter.web.vo.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 协议包发布出参（管理后台）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class AgreementPublishResponse {

    private String bundleId;

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private String privacyRevisionId;

    private String userRevisionId;

    private String publishedBy;

    private Instant publishedAt;

    private String requestId;

    private String requestHash;

    private String expectedBundleId;
}
