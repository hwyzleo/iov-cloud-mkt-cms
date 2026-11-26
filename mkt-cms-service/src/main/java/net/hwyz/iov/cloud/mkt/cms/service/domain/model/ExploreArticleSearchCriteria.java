package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 探索页文章管理检索条件（空串视为不过滤）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExploreArticleSearchCriteria {

    private final String sourceType;

    private final String status;

    private final String title;
}
