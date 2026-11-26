package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticle;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreArticleRevision;

/**
 * 探索页文章管理视图（根实体 + 当前/最新修订）。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class ExploreArticleView {

    private final ExploreArticle article;

    private final ExploreArticleRevision revision;
}
