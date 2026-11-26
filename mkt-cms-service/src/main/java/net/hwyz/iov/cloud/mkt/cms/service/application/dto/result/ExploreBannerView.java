package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBanner;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExploreBannerRevision;

/**
 * 探索页 Banner 管理视图（根实体 + 修订）。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class ExploreBannerView {

    private final ExploreBanner banner;

    private final ExploreBannerRevision revision;
}
