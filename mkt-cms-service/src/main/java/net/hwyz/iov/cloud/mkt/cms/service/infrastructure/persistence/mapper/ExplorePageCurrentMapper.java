package net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.persistence.po.ExplorePageCurrentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/**
 * 探索页频道当前版本指针 Mapper。
 *
 * @author hwyz_leo
 */
@Mapper
public interface ExplorePageCurrentMapper {

    /**
     * 查询全部频道当前指针
     *
     * @return 当前指针列表
     */
    List<ExplorePageCurrentPo> selectAll();

    /**
     * 查询频道当前指针
     *
     * @param channelCode 频道编码
     * @return 当前指针
     */
    ExplorePageCurrentPo selectByChannel(@Param("channelCode") String channelCode);

    /**
     * 锁读频道当前指针
     *
     * @param channelCode 频道编码
     * @return 当前指针
     */
    ExplorePageCurrentPo selectByChannelForUpdate(@Param("channelCode") String channelCode);

    /**
     * 初始化空当前指针（唯一键竞争时由调用方重试）
     *
     * @param channelCode 频道编码
     * @param now         服务端时间
     * @return 影响行数
     */
    int insertEmpty(@Param("channelCode") String channelCode, @Param("now") Instant now);

    /**
     * 按乐观锁切换当前指针
     *
     * @param channelCode       频道编码
     * @param expectedLockVersion 期望指针版本
     * @param releaseId         新版本标识
     * @param now               服务端时间
     * @return 影响行数
     */
    int switchPointer(@Param("channelCode") String channelCode,
                      @Param("expectedLockVersion") int expectedLockVersion,
                      @Param("releaseId") String releaseId,
                      @Param("now") Instant now);

    /**
     * 按乐观锁清空当前指针
     *
     * @param channelCode         频道编码
     * @param expectedLockVersion 期望指针版本
     * @param now                 服务端时间
     * @return 影响行数
     */
    int clearPointer(@Param("channelCode") String channelCode,
                     @Param("expectedLockVersion") int expectedLockVersion,
                     @Param("now") Instant now);

    /**
     * 调度激活：按 releaseId 比较后切换指针
     *
     * @param channelCode      频道编码
     * @param expectedReleaseId 期望当前版本（可为空）
     * @param newReleaseId      新版本标识
     * @param now              服务端时间
     * @return 影响行数
     */
    int activate(@Param("channelCode") String channelCode,
                 @Param("expectedReleaseId") String expectedReleaseId,
                 @Param("newReleaseId") String newReleaseId,
                 @Param("now") Instant now);

    /**
     * 调度到期：按 releaseId 比较后清空指针
     *
     * @param channelCode       频道编码
     * @param expectedReleaseId  期望当前版本
     * @param now                服务端时间
     * @return 影响行数
     */
    int expire(@Param("channelCode") String channelCode,
               @Param("expectedReleaseId") String expectedReleaseId,
               @Param("now") Instant now);
}
