package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.AgreementType;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.AgreementException;

import java.time.Instant;

/**
 * 协议发布包（不可变成套快照）。
 * <p>
 * MKT-CMS-DSN-CR-001 §5：一个发布包必须且仅包含一份隐私政策与一份用户协议，
 * 两份修订范围必须与命令范围一致；包不可变，历史包仍是已发布事实。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementBundle {

    private Long id;

    private String bundleId;

    private AgreementScope scope;

    private String privacyRevisionId;

    private String userRevisionId;

    private String publishedBy;

    private Instant publishedAt;

    private String requestId;

    private String requestHash;

    private String expectedBundleId;

    /**
     * 成套发布包构建（域内校验：两份修订类型正确且同范围）
     *
     * @param bundleId         服务端分配的发布包标识
     * @param scope            规范化适用范围
     * @param privacy          隐私政策修订
     * @param userAgreement    用户协议修订
     * @param requestId        幂等请求标识
     * @param requestHash      业务命令哈希
     * @param expectedBundleId 期望的当前包标识（首次发布为 null）
     * @param operator         发布人（来自安全上下文）
     * @param now              服务端时间
     * @return 发布包
     */
    public static AgreementBundle publishOf(String bundleId, AgreementScope scope,
                                            AgreementRevision privacy, AgreementRevision userAgreement,
                                            String requestId, String requestHash, String expectedBundleId,
                                            String operator, Instant now) {
        if (privacy == null || userAgreement == null) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_REVISION_NOT_FOUND);
        }
        if (privacy.getType() != AgreementType.PRIVACY_POLICY) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "隐私政策位必须引用 PRIVACY_POLICY 修订");
        }
        if (userAgreement.getType() != AgreementType.USER_AGREEMENT) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "用户协议位必须引用 USER_AGREEMENT 修订");
        }
        if (!scope.equals(privacy.getScope()) || !scope.equals(userAgreement.getScope())) {
            throw new AgreementException(AgreementErrorCode.AGREEMENT_INVALID_ARGUMENT,
                    "发布范围必须与两份修订范围完全一致");
        }
        return AgreementBundle.builder()
                .bundleId(bundleId)
                .scope(scope)
                .privacyRevisionId(privacy.getRevisionId())
                .userRevisionId(userAgreement.getRevisionId())
                .publishedBy(operator)
                .publishedAt(now)
                .requestId(requestId)
                .requestHash(requestHash)
                .expectedBundleId(expectedBundleId)
                .build();
    }

    /**
     * 发布前提是否与传入的实际当前包一致
     *
     * @param actualCurrentBundleId 实际当前包标识（可为 null）
     * @return 一致返回 true
     */
    public boolean matchesCurrent(String actualCurrentBundleId) {
        return expectedBundleId == null
                ? actualCurrentBundleId == null
                : expectedBundleId.equals(actualCurrentBundleId);
    }
}
