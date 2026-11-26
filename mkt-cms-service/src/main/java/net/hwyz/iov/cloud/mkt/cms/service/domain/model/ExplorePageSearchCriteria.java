package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 探索页发布历史检索条件（空串视为不过滤）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExplorePageSearchCriteria {

    private final String channelCode;

    private final String status;
}
