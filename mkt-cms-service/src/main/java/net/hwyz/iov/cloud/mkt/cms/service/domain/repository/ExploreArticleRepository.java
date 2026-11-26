package net.hwyz.iov.cloud.mkt.cms.service.domain.repository;

import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleSearchCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 探索页文章仓储（根实体与不可变修订）。
 *
 * @author hwyz_leo
 */
public interface ExploreArticleRepository {

    void insert(ExploreArticle article);

    int update(ExploreArticle article, int expectedLockVersion);

    Optional<ExploreArticle> findByArticleId(String articleId);

    Optional<ExploreArticle> findByArticleIdForUpdate(String articleId);

    List<ExploreArticle> findByIds(Collection<String> articleIds);

    List<ExploreArticle> search(ExploreArticleSearchCriteria criteria);

    void insertRevision(ExploreArticleRevision revision);

    int updateDraftRevision(ExploreArticleRevision revision);

    int publishRevision(ExploreArticleRevision revision);

    Optional<ExploreArticleRevision> findRevision(String revisionId);

    Optional<ExploreArticleRevision> findLatestDraft(String articleId);

    Optional<ExploreArticleRevision> findLatestRevision(String articleId);
}
