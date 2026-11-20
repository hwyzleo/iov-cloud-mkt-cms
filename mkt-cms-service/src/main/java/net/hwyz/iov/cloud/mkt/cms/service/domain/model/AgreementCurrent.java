package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 当前发布指针（五维 Scope 唯一）。
 * <p>
 * MKT-CMS-DSN-CR-001 §5.2：CURRENT 由当前指针派生，切换与发布包创建在同一事务内完成；
 * 本期不开放撤回或清空当前指针，bundleId 为 null 只表示该范围尚无当前包。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementCurrent {

    private AgreementScope scope;

    private String bundleId;

    private int lockVersion;

    private Instant updatedAt;

    /**
     * 尚未发布时的空指针
     *
     * @param scope 适用范围
     * @param now   服务端时间
     * @return 空指针
     */
    public static AgreementCurrent empty(AgreementScope scope, Instant now) {
        return AgreementCurrent.builder()
                .scope(scope)
                .bundleId(null)
                .lockVersion(0)
                .updatedAt(now)
                .build();
    }

    /**
     * 切换到新发布包（指针版本递增）
     *
     * @param newBundleId 新发布包标识
     * @param now         服务端时间
     * @return 切换后的指针（不修改原对象）
     */
    public AgreementCurrent switchTo(String newBundleId, Instant now) {
        return AgreementCurrent.builder()
                .scope(scope)
                .bundleId(newBundleId)
                .lockVersion(lockVersion + 1)
                .updatedAt(now)
                .build();
    }
}
