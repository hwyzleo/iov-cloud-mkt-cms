package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;
import net.hwyz.iov.cloud.mkt.cms.service.common.util.HexSha256;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 协议修订（不可变内容快照）。
 * <p>
 * MKT-CMS-DSN-CR-001 §3.1/§3.2：定稿对已保存的安全 HTML 精确字节生成 SHA-256，
 * 持久化正文、格式、摘要与清洗策略版本；返回与历史回读使用同一份快照，
 * 不在读取时按新规则重写正文；修订插入后不可修改或删除。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementRevision {

    /**
     * 摘要前缀（与下游约定 sha256: 前缀；库内单独存 64 位十六进制）
     */
    public static final String HASH_PREFIX = "sha256:";

    private Long id;

    private String revisionId;

    private String agreementCode;

    private AgreementType type;

    private AgreementScope scope;

    private String version;

    private LocalDate agreementDate;

    private String title;

    private String contentType;

    private String body;

    private String contentHash;

    private String sanitizerVersion;

    private String sourceDraftId;

    private int sourceDraftVersion;

    private String frozenBy;

    private Instant frozenAt;

    /**
     * 以草稿快照生成不可变修订
     *
     * @param draft        草稿
     * @param revisionId   服务端分配的修订标识
     * @param agreementCode 稳定协议身份标识
     * @param operator     定稿人（来自安全上下文）
     * @param now          服务端时间
     * @return 不可变修订
     */
    public static AgreementRevision freezeOf(AgreementDraft draft, String revisionId,
                                             String agreementCode, String operator, Instant now) {
        if (draft == null) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_DRAFT_NOT_FOUND);
        }
        if (draft.getBody() == null || draft.getBody().isEmpty()) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT, "协议正文不能为空");
        }
        return AgreementRevision.builder()
                .revisionId(revisionId)
                .agreementCode(agreementCode)
                .type(draft.getType())
                .scope(draft.getScope())
                .version(draft.getVersion())
                .agreementDate(draft.getAgreementDate())
                .title(draft.getTitle())
                .contentType(draft.getContentType())
                .body(draft.getBody())
                .contentHash(HexSha256.ofUtf8(draft.getBody()))
                .sanitizerVersion(draft.getSanitizerVersion())
                .sourceDraftId(draft.getDraftId())
                .sourceDraftVersion(draft.getLockVersion())
                .frozenBy(operator)
                .frozenAt(now)
                .build();
    }

    /**
     * 对外摘要形式（带 sha256: 前缀）
     *
     * @return 形如 sha256:&lt;64 位十六进制&gt;
     */
    public String prefixedContentHash() {
        return contentHash == null ? null : HASH_PREFIX + contentHash;
    }
}
