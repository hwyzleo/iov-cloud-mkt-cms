package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreChannelRevision;

/**
 * 探索页频道管理视图（根实体状态 + 草稿/当前发布修订）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@AllArgsConstructor
public class ExploreChannelView {

    private final String channelCode;

    private final ExploreChannelRevision draft;

    private final ExploreChannelRevision published;

    private final boolean hasUnpublishedChanges;

    private final boolean deletable;

    private final int lockVersion;
}
