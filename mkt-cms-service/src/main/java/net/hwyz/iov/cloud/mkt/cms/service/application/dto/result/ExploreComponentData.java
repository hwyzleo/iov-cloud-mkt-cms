package net.hwyz.iov.cloud.mkt.cms.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageComponent;
import net.hwyz.iov.cloud.mkt.cms.service.domain.model.ExplorePageItem;

import java.util.List;

/**
 * 探索页组件与条目视图。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public class ExploreComponentData {

    private final ExplorePageComponent component;

    private final List<ExplorePageItem> items;
}
