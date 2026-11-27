package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;

/**
 * 探索页频道独立发布命令（MKT-CMS-DSN-CR-005 §4.2 / D-019）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExploreChannelPublishCmd {

    private final String channelCode;

    private final String requestId;

    private final String expectedCurrentRevisionId;

    private final Integer lockVersion;

    private final String operator;
}
