package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopic;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicItem;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTopicRevision;

import java.util.List;

/**
 * 探索页专题管理视图（根实体 + 修订 + 有序文章条目）。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class ExploreTopicView {

    private final ExploreTopic topic;

    private final ExploreTopicRevision revision;

    private final List<ExploreTopicItem> items;
}
