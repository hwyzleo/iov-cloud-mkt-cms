package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementCurrent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;

import java.time.Instant;
import java.util.Optional;

/**
 * 协议当前指针仓储（域层接口，基础设施层实现）。
 *
 * @author hwyz_leo
 */
public interface AgreementCurrentRepository {

    /**
     * 行锁读取当前指针（SELECT ... FOR UPDATE），锁内必须能看到最新已提交数据
     *
     * @param scope 规范化适用范围
     * @return 当前指针；该范围尚无指针行时为空
     */
    Optional<AgreementCurrent> lockByScope(AgreementScope scope);

    /**
     * 建立空指针行（首次发布前建立；并发建立由 Scope 唯一键竞争兜底）
     *
     * @param scope 规范化适用范围
     * @param now   服务端时间
     */
    void insertEmpty(AgreementScope scope, Instant now);

    /**
     * 切换当前指针并递增指针版本
     *
     * @param scope               规范化适用范围
     * @param expectedLockVersion 期望的原指针版本
     * @param newBundleId         新发布包标识
     * @param now                 服务端时间
     * @return 影响行数，0 表示指针已被并发切换
     */
    int switchPointer(AgreementScope scope, int expectedLockVersion, String newBundleId, Instant now);
}
