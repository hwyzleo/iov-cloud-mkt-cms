package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成套发布协议命令。
 * <p>
 * MKT-CMS-DSN-CR-001 §5.1：requestId 与 expectedBundleId 都必须出现；
 * 首次发布 expectedBundleId 显式为 null 表示“预期尚无当前包”，由调用方
 * {@code expectedBundleIdPresent} 显式标记字段存在性，避免框架层把“缺失”误当“null”。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishAgreementBundleCmd {

    private String scene;

    private String market;

    private String locale;

    private String brandCode;

    private String carLineCode;

    private String privacyRevisionId;

    private String userAgreementRevisionId;

    private String requestId;

    /**
     * 期望的当前包标识；首次发布为 null
     */
    private String expectedBundleId;

    /**
     * 请求体是否显式出现了 expectedBundleId 字段
     */
    private boolean expectedBundleIdPresent;

    private String operator;
}
