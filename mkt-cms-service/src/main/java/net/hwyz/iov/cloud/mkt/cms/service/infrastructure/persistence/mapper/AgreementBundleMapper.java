package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementBundlePo;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.AgreementSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.CurrentBundleViewPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 协议发布包 Mapper。
 *
 * @author hwyz_leo
 */
@Mapper
public interface AgreementBundleMapper {

    /**
     * 新增不可变发布包
     *
     * @param po 发布包
     * @return 影响行数
     */
    int insertPo(AgreementBundlePo po);

    /**
     * 按发布包标识查询
     *
     * @param bundleId 发布包标识
     * @return 发布包
     */
    AgreementBundlePo selectByBundleId(@Param("bundleId") String bundleId);

    /**
     * 按幂等请求标识查询
     *
     * @param requestId 幂等请求标识
     * @return 发布包
     */
    AgreementBundlePo selectByRequestId(@Param("requestId") String requestId);

    /**
     * 锁读幂等请求标识（锁等待后必须能看到最新已提交数据，不能依赖旧事务快照）
     *
     * @param requestId 幂等请求标识
     * @return 发布包
     */
    AgreementBundlePo selectByRequestIdForUpdate(@Param("requestId") String requestId);

    /**
     * 一次 JOIN 读取当前完整发布包
     *
     * @param scene       场景
     * @param market      市场
     * @param locale      语言
     * @param brandCode   品牌
     * @param carLineCode 车系
     * @return 当前发布包视图
     */
    /**
     * 条件检索历史发布包
     *
     * @param criteria 检索条件
     * @return 发布包列表
     */
    List<AgreementBundlePo> selectPoByCriteria(@Param("criteria") AgreementSearchCriteria criteria);

    CurrentBundleViewPo selectCurrentView(@Param("scene") String scene,
                                          @Param("market") String market,
                                          @Param("locale") String locale,
                                          @Param("brandCode") String brandCode,
                                          @Param("carLineCode") String carLineCode);
}
