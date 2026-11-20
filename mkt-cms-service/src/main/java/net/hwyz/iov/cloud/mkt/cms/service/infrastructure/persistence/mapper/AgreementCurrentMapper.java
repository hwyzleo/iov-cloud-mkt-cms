package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementCurrentPo;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.AgreementOperationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

/**
 * 协议当前指针与操作留痕 Mapper。
 *
 * @author hwyz_leo
 */
@Mapper
public interface AgreementCurrentMapper {

    /**
     * 建立空指针行（并发建立由 Scope 唯一键竞争兜底）
     *
     * @param po 空指针
     * @return 影响行数
     */
    int insertPo(AgreementCurrentPo po);

    /**
     * 行锁读取当前指针（SELECT ... FOR UPDATE）
     *
     * @param scene       场景
     * @param market      市场
     * @param locale      语言
     * @param brandCode   品牌
     * @param carLineCode 车系
     * @return 当前指针
     */
    AgreementCurrentPo selectForUpdate(@Param("scene") String scene,
                                       @Param("market") String market,
                                       @Param("locale") String locale,
                                       @Param("brandCode") String brandCode,
                                       @Param("carLineCode") String carLineCode);

    /**
     * 切换当前指针并递增指针版本
     *
     * @param scene               场景
     * @param market              市场
     * @param locale              语言
     * @param brandCode           品牌
     * @param carLineCode         车系
     * @param newBundleId         新发布包标识
     * @param expectedLockVersion 期望的原指针版本
     * @param now                 更新时间（UTC）
     * @return 影响行数
     */
    int switchPointer(@Param("scene") String scene,
                      @Param("market") String market,
                      @Param("locale") String locale,
                      @Param("brandCode") String brandCode,
                      @Param("carLineCode") String carLineCode,
                      @Param("newBundleId") String newBundleId,
                      @Param("expectedLockVersion") int expectedLockVersion,
                      @Param("now") Instant now);

    /**
     * 新增操作留痕
     *
     * @param po 操作留痕
     * @return 影响行数
     */
    int insertOperationPo(AgreementOperationPo po);
}
