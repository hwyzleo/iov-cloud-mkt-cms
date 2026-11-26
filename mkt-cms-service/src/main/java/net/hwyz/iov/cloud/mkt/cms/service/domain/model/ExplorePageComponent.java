package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探索页页面组件。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.2 / D-011：首版每类组件每页最多一个，使用显式排序。
 *
 * @author hwyz_leo
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePageComponent {

    private Long id;

    private String componentId;

    private String pageId;

    private ExploreComponentType componentType;

    private int sortNo;

    private Map<String, Object> config;

    /**
     * 组件配置视图
     *
     * @return 有序 Map
     */
    public Map<String, Object> configView() {
        return config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
    }
}
