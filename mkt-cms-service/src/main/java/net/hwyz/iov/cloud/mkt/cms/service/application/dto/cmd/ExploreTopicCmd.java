package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;

import java.util.List;

/**
 * 探索页专题新建/修改命令（文章顺序即列表顺序）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExploreTopicCmd {

    private final String topicId;

    private final String name;

    private final String summary;

    private final ExploreImage cover;

    private final List<ExploreImage> images;

    private final ExploreTarget target;

    private final List<String> articleRevisionIds;

    private final Integer lockVersion;

    private final String operator;
}
