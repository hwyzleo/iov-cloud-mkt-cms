package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探索页专题文章条目（人工顺序 + 展示快照）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExploreTopicItem {

    private Long id;

    private String topicRevisionId;

    private String articleRevisionId;

    private int sortNo;

    private Map<String, Object> displaySnapshot;

    private String createBy;

    private Instant createTime;

    private String modifyBy;

    private Instant modifyTime;

    /**
     * 卡片展示视图（附条目引用标识）
     * <p>
     * MKT-CMS-DSN-CR-003 §6.2 手机端卡片契约要求每条卡片携带非空 {@code itemId} 与实际 {@code sortNo}；
     * 专题条目以文章修订标识作为稳定且唯一的条目引用标识，避免同一文章的多个修订产生重复 key。
     *
     * @return 有序 Map
     */
    public Map<String, Object> displayView() {
        Map<String, Object> view = displaySnapshot == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(displaySnapshot);
        view.put("itemId", articleRevisionId);
        view.put("sortNo", sortNo);
        return view;
    }
}
