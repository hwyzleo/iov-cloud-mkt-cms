package net.hwyz.iov.cloud.mkt.cms.service.application.dto.cmd;

import lombok.Builder;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreImage;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreTarget;

/**
 * 探索页 Banner 新建/修改命令。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
public class ExploreBannerCmd {

    private final String bannerId;

    private final String title;

    private final ExploreImage image;

    private final ExploreTarget target;

    private final Integer defaultSortNo;

    private final Integer lockVersion;

    private final String operator;
}
