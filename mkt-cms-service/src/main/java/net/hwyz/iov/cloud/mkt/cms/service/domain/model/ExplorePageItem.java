package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreItemType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探索页页面条目。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.2 / D-016：引用具体已发布修订并固定展示快照；
 * 排序键为 {@code sortNo + itemId}，发布版本条目不可改删。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePageItem {

    private Long id;

    private String itemId;

    private String componentId;

    private ExploreItemType itemType;

    private String refId;

    private String refRevisionId;

    private int sortNo;

    private Instant startAt;

    private Instant endAt;

    private ExploreTarget target;

    private Map<String, Object> displaySnapshot;

    /**
     * 卡片展示视图（附条目引用标识）
     *
     * @return 有序 Map
     */
    public Map<String, Object> displayView() {
        Map<String, Object> view = displaySnapshot == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(displaySnapshot);
        view.put("itemId", itemId);
        view.put("sortNo", sortNo);
        return view;
    }
}
