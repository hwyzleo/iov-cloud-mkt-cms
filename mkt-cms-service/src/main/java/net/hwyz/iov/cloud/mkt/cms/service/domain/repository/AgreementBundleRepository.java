package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementBundle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementScope;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.CurrentBundleView;

import java.util.List;
import java.util.Optional;

/**
 * 协议发布包仓储（域层接口，基础设施层实现）。
 *
 * @author hwyz_leo
 */
public interface AgreementBundleRepository {

    /**
     * 新增不可变发布包
     *
     * @param bundle 发布包
     * @return 落库后的发布包
     */
    AgreementBundle insert(AgreementBundle bundle);

    /**
     * 按发布包标识查询
     *
     * @param bundleId 发布包标识
     * @return 发布包
     */
    Optional<AgreementBundle> findByBundleId(String bundleId);

    /**
     * 按幂等请求标识查询
     *
     * @param requestId 幂等请求标识
     * @return 发布包
     */
    Optional<AgreementBundle> findByRequestId(String requestId);

    /**
     * 锁读幂等请求标识（用于获得发布锁后重新检查幂等结果）
     *
     * @param requestId 幂等请求标识
     * @return 发布包
     */
    Optional<AgreementBundle> findByRequestIdForUpdate(String requestId);

    /**
     * 一次一致性读取当前完整发布包（指针 + 发布包 + 两份修订）
     *
     * @param scope 规范化适用范围
     * @return 当前发布包视图
     */
    Optional<CurrentBundleView> findCurrentView(AgreementScope scope);

    /**
     * 条件检索历史发布包
     *
     * @param criteria 检索条件
     * @return 发布包列表
     */
    List<AgreementBundle> search(AgreementSearchCriteria criteria);
}
