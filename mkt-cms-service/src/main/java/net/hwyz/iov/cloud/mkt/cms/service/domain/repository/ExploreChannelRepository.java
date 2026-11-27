package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannel;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 探索页频道仓储（根实体 + 草稿/发布修订）。
 *
 * @author hwyz_leo
 */
public interface ExploreChannelRepository {

    /**
     * 全部频道根实体（按编码稳定排序）
     *
     * @return 频道列表
     */
    List<ExploreChannel> findAll();

    /**
     * 按编码查询根实体
     *
     * @param channelCode 频道编码
     * @return 频道
     */
    Optional<ExploreChannel> findByCode(String channelCode);

    /**
     * 按编码查询并锁定根实体
     *
     * @param channelCode 频道编码
     * @return 频道
     */
    Optional<ExploreChannel> findByCodeForUpdate(String channelCode);

    /**
     * 已启用且存在当前有效页面的频道（读取当前发布修订的排序与名称）
     *
     * @param now 服务端时间
     * @return 频道列表
     */
    List<ExploreChannel> findEnabledWithCurrent(Instant now);

    /**
     * 创建频道根实体
     *
     * @param channel 频道
     */
    void insert(ExploreChannel channel);

    /**
     * 按乐观锁更新频道根实体
     *
     * @param channel             频道
     * @param expectedLockVersion 期望乐观锁版本
     * @return 影响行数
     */
    int update(ExploreChannel channel, int expectedLockVersion);

    /**
     * 按乐观锁删除频道根实体
     *
     * @param channelCode         频道编码
     * @param expectedLockVersion 期望乐观锁版本
     * @return 影响行数
     */
    int deleteByCode(String channelCode, int expectedLockVersion);

    /**
     * 删除频道全部修订
     *
     * @param channelCode 频道编码
     */
    void deleteRevisions(String channelCode);

    /**
     * 创建修订
     *
     * @param revision 修订
     */
    void insertRevision(ExploreChannelRevision revision);

    /**
     * 更新草稿修订（仅 DRAFT 可改）
     *
     * @param revision 草稿修订
     * @return 影响行数
     */
    int updateDraftRevision(ExploreChannelRevision revision);

    /**
     * 草稿定稿为 PUBLISHED（仅 DRAFT 可发布）
     *
     * @param revision 已发布修订
     * @return 影响行数
     */
    int publishRevision(ExploreChannelRevision revision);

    /**
     * 按修订标识查询
     *
     * @param revisionId 修订标识
     * @return 修订
     */
    Optional<ExploreChannelRevision> findRevision(String revisionId);

    /**
     * 按修订标识批量查询
     *
     * @param revisionIds 修订标识集合
     * @return 修订标识 → 修订
     */
    Map<String, ExploreChannelRevision> findRevisionsByIds(Collection<String> revisionIds);

    /**
     * 最新修订（任意状态）
     *
     * @param channelCode 频道编码
     * @return 修订
     */
    Optional<ExploreChannelRevision> findLatestRevision(String channelCode);

    /**
     * 最新草稿修订
     *
     * @param channelCode 频道编码
     * @return 草稿修订
     */
    Optional<ExploreChannelRevision> findLatestDraft(String channelCode);

    /**
     * 当前已发布修订
     *
     * @param channelCode 频道编码
     * @return 当前已发布修订
     */
    Optional<ExploreChannelRevision> findCurrentPublishedRevision(String channelCode);

    /**
     * 按幂等请求标识查询已发布修订
     *
     * @param requestId 请求标识
     * @return 修订
     */
    Optional<ExploreChannelRevision> findByRequestId(String requestId);

    /**
     * 是否存在引用该频道的页面（草稿或任意历史发布版本）
     *
     * @param channelCode 频道编码
     * @return 是否存在引用
     */
    boolean existsPageReference(String channelCode);

    /**
     * 批量查询被页面引用的频道编码
     *
     * @param channelCodes 频道编码集合
     * @return 被引用的频道编码
     */
    Set<String> findReferencedChannelCodes(Collection<String> channelCodes);
}
