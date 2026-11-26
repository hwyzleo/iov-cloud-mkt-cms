package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探索页页面视图（手机端当前页与管理预览共用组件契约）。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePageView {

    /**
     * 契约版本
     */
    public static final int SCHEMA_VERSION = 1;

    private String channelCode;

    private String channelName;

    private String releaseId;

    private String publishedAt;

    private boolean preview;

    private List<ExploreComponentView> components;

    /**
     * 对外 Map
     *
     * @return 有序 Map
     */
    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("schemaVersion", SCHEMA_VERSION);
        Map<String, Object> channel = new LinkedHashMap<>();
        channel.put("code", channelCode);
        channel.put("name", channelName);
        view.put("channel", channel);
        view.put("releaseId", releaseId);
        view.put("publishedAt", publishedAt);
        if (preview) {
            view.put("preview", true);
        }
        view.put("components", components.stream().map(ExploreComponentView::toView).toList());
        return view;
    }
}
