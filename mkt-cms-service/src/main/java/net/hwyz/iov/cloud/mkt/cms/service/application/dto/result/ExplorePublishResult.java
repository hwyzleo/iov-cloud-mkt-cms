package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;

/**
 * 探索页发布结果。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class ExplorePublishResult {

    private final ExplorePage page;
}
