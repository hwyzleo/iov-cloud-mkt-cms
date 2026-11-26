package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;

/**
 * 探索页频道维护命令。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExploreChannelCmd {

    private final String channelCode;

    private final String name;

    private final Integer sortNo;

    private final String status;

    private final Integer lockVersion;

    private final String operator;
}
