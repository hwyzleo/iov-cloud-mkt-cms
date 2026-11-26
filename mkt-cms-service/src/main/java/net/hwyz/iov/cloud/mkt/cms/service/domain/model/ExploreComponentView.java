package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页组件视图（手机端与管理预览共用）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreComponentView {

    private String type;

    private String componentId;

    private int sortNo;

    private Map<String, Object> config;

    private List<Map<String, Object>> items;

    private boolean hasMore;

    private String nextCursor;

    /**
     * 对外 Map（省略空扩展字段，保持契约稳定）
     *
     * @return 有序 Map
     */
    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("type", type);
        view.put("componentId", componentId);
        view.put("sortNo", sortNo);
        if (config != null && !config.isEmpty()) {
            view.putAll(config);
        }
        view.put("items", items);
        if (hasMore) {
            view.put("hasMore", true);
            view.put("nextCursor", nextCursor);
        }
        return view;
    }
}
