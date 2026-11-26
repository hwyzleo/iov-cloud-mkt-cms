package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;

/**
 * 探索页内容发布/下线命令。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExploreContentPublishCmd {

    private final String objectId;

    private final String revisionId;

    private final String operator;
}
