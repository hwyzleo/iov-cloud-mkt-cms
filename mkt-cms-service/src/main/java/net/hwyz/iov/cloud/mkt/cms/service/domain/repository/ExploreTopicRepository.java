package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreContentSearchCriteria;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 探索页专题仓储（根实体、修订与有序文章条目）。
 *
 * @author hwyz_leo
 */
public interface ExploreTopicRepository {

    void insert(ExploreTopic topic);

    int update(ExploreTopic topic, int expectedLockVersion);

    Optional<ExploreTopic> findByTopicId(String topicId);

    Optional<ExploreTopic> findByTopicIdForUpdate(String topicId);

    List<ExploreTopic> search(ExploreContentSearchCriteria criteria);

    void insertRevision(ExploreTopicRevision revision);

    int updateDraftRevision(ExploreTopicRevision revision);

    int publishRevision(ExploreTopicRevision revision);

    Optional<ExploreTopicRevision> findRevision(String revisionId);

    Optional<ExploreTopicRevision> findLatestDraft(String topicId);

    Optional<ExploreTopicRevision> findLatestRevision(String topicId);

    void replaceItems(String topicRevisionId, List<ExploreTopicItem> items);

    List<ExploreTopicItem> listItems(String topicRevisionId);

    /**
     * 指定专题修订的专题详情页 keyset 分页（按人工顺序）
     *
     * @param topicRevisionId 专题修订标识
     * @param lastSortNo      游标排序（首页为空）
     * @param limit           页大小
     * @return 条目列表
     */
    List<ExploreTopicItem> listItemsAfter(String topicRevisionId, Integer lastSortNo, int limit);

    /**
     * 替换专题修订图集（草稿修订专用；已发布修订不可改删）
     *
     * @param topicRevisionId 专题修订标识
     * @param images          有序图集
     */
    void replaceRevisionImages(String topicRevisionId, List<ExploreImage> images);

    /**
     * 查询专题修订图集
     *
     * @param topicRevisionId 专题修订标识
     * @return 有序图集
     */
    List<ExploreImage> findRevisionImages(String topicRevisionId);

    /**
     * 批量查询专题修订图集
     *
     * @param topicRevisionIds 专题修订标识集合
     * @return 修订标识 → 有序图集
     */
    Map<String, List<ExploreImage>> findRevisionImagesByIds(Collection<String> topicRevisionIds);

    /**
     * 批量查询专题根实体
     *
     * @param topicIds 专题标识集合
     * @return 专题列表
     */
    List<ExploreTopic> findByIds(java.util.Collection<String> topicIds);
}
