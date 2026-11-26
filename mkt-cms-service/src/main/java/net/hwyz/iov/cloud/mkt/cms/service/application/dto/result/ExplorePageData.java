package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePage;

import java.util.List;

/**
 * 探索页页面内容视图（页面可为空，表示尚无草稿/版本）。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class ExplorePageData {

    private final ExplorePage page;

    private final List<ExploreComponentData> components;
}
