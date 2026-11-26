package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 探索页频道发布命令。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExplorePagePublishCmd {

    private final String channelCode;

    private final String requestId;

    private final String expectedReleaseId;

    private final String publishType;

    private final Instant effectiveAt;

    private final Instant expireAt;

    private final String operator;
}
