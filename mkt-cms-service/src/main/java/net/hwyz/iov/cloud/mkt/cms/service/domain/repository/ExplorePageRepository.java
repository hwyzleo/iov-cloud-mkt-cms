package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageSearchCriteria;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 探索页频道页面仓储（草稿、不可变发布版本、组件与条目）。
 *
 * @author hwyz_leo
 */
public interface ExplorePageRepository {

    void insert(ExplorePage page);

    int updateDraft(ExplorePage page, int expectedLockVersion);

    int updateStatus(ExplorePage page);

    Optional<ExplorePage> findById(String pageId);

    Optional<ExplorePage> findByIdForUpdate(String pageId);

    Optional<ExplorePage> findDraft(String channelCode);

    Optional<ExplorePage> findDraftForUpdate(String channelCode);

    Optional<ExplorePage> findByRequestId(String requestId);

    Optional<ExplorePage> findScheduled(String channelCode);

    Optional<ExplorePage> findCurrentPage(String channelCode);

    int maxPageVersion(String channelCode);

    List<ExplorePage> searchReleases(ExplorePageSearchCriteria criteria);

    void deletePageContent(String pageId);

    void insertComponent(ExplorePageComponent component);

    void insertItem(ExplorePageItem item);

    List<ExplorePageComponent> listComponents(String pageId);

    List<ExplorePageItem> listItems(String componentId);

    List<ExplorePageItem> listItemsByComponentIds(List<String> componentIds);

    List<ExplorePageItem> feedPage(String componentId, Integer lastSortNo, String lastItemId, int limit);

    /**
     * 到期待激活的定时版本
     *
     * @param now   服务端时间
     * @param limit 批量上限
     * @return 待激活版本
     */
    List<ExplorePage> findDueSchedules(Instant now, int limit);

    /**
     * 已到期且仍为当前指针的版本
     *
     * @param now   服务端时间
     * @param limit 批量上限
     * @return 到期版本
     */
    List<ExplorePage> findExpiredCurrent(Instant now, int limit);
}
