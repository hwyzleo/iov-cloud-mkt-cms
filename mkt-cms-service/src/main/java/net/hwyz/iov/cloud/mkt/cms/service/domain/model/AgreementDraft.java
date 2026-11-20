package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 协议草稿（可多次编辑，乐观锁防覆盖）。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.2/§4.2：草稿修改不改变既有修订；草稿版本由乐观锁版本承载，
 * 定稿以“草稿 + 乐观锁版本”为幂等依据；正文一律保存清洗后的安全 HTML。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementDraft {

    private static final int TITLE_MAX_LENGTH = 255;

    private static final int VERSION_MAX_LENGTH = 64;

    private String draftId;

    private AgreementType type;

    private AgreementScope scope;

    private String title;

    private String version;

    private LocalDate agreementDate;

    private String contentType;

    private String body;

    private String sanitizerVersion;

    private int lockVersion;

    private String createdBy;

    private Instant createdAt;

    private String updatedBy;

    private Instant updatedAt;

    /**
     * 新建草稿
     *
     * @param draftId       服务端分配的草稿标识
     * @param type          协议类型
     * @param scope         规范化适用范围
     * @param title         标题
     * @param version       业务展示版本
     * @param agreementDate 协议日期
     * @param html          清洗后的安全正文
     * @param operator      操作人（来自安全上下文）
     * @param now           服务端时间
     * @return 草稿
     */
    public static AgreementDraft create(String draftId, AgreementType type, AgreementScope scope,
                                        String title, String version, LocalDate agreementDate,
                                        SanitizedHtml html, String operator, Instant now) {
        validateMetadata(title, version, agreementDate);
        if (html == null || html.getBody() == null) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议正文不能为空");
        }
        return AgreementDraft.builder()
                .draftId(draftId)
                .type(type)
                .scope(scope)
                .title(title.trim())
                .version(version.trim())
                .agreementDate(agreementDate)
                .contentType(SanitizedHtml.CONTENT_TYPE)
                .body(html.getBody())
                .sanitizerVersion(html.getSanitizerVersion())
                .lockVersion(0)
                .createdBy(operator)
                .createdAt(now)
                .updatedBy(operator)
                .updatedAt(now)
                .build();
    }

    /**
     * 生成一次编辑后的草稿（乐观锁版本递增，范围与类型不可变）
     *
     * @param newTitle         新标题，null 表示不变
     * @param newVersion       新业务版本，null 表示不变
     * @param newAgreementDate 新协议日期，null 表示不变
     * @param newHtml          新清洗正文，null 表示不变
     * @param operator         操作人
     * @param now              服务端时间
     * @return 编辑后的草稿
     */
    public AgreementDraft edited(String newTitle, String newVersion, LocalDate newAgreementDate,
                                 SanitizedHtml newHtml, String operator, Instant now) {
        String targetTitle = newTitle == null ? title : newTitle;
        String targetVersion = newVersion == null ? version : newVersion;
        LocalDate targetDate = newAgreementDate == null ? agreementDate : newAgreementDate;
        validateMetadata(targetTitle, targetVersion, targetDate);
        return AgreementDraft.builder()
                .draftId(draftId)
                .type(type)
                .scope(scope)
                .title(targetTitle.trim())
                .version(targetVersion.trim())
                .agreementDate(targetDate)
                .contentType(newHtml == null ? contentType : SanitizedHtml.CONTENT_TYPE)
                .body(newHtml == null ? body : newHtml.getBody())
                .sanitizerVersion(newHtml == null ? sanitizerVersion : newHtml.getSanitizerVersion())
                .lockVersion(lockVersion + 1)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .updatedBy(operator)
                .updatedAt(now)
                .build();
    }

    private static void validateMetadata(String title, String version, LocalDate agreementDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议标题不能为空");
        }
        if (title.trim().length() > TITLE_MAX_LENGTH) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "协议标题长度不得超过 " + TITLE_MAX_LENGTH);
        }
        if (version == null || version.trim().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议业务版本不能为空");
        }
        if (version.trim().length() > VERSION_MAX_LENGTH) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "协议业务版本长度不得超过 " + VERSION_MAX_LENGTH);
        }
        if (agreementDate == null) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议日期不能为空");
        }
    }
}
