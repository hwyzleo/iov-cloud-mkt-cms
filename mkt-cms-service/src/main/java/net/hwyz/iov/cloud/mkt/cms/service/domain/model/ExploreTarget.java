package net.hwyz.iov.cloud.mkt.cms.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探索页跳转目标（判别联合）。
 * <p>
 * MKT-CMS-DSN-CR-003 §4.2 / D-012：ARTICLE 必须有 articleId + revisionId；
 * TOPIC 必须有 topicId + revisionId；H5 必须有白名单 origin 的绝对 HTTPS URL；
 * NATIVE 必须有元数据下发的 routeCode 与受控参数键。
 *
 * @author hwyz_leo
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreTarget {

    private ExploreTargetType type;

    /**
     * ARTICLE / TOPIC 目标业务标识
     */
    private String refId;

    /**
     * ARTICLE / TOPIC 目标修订标识
     */
    private String revisionId;

    /**
     * H5 目标 URL
     */
    private String url;

    /**
     * NATIVE 路由编码
     */
    private String routeCode;

    /**
     * NATIVE 受控参数（仅字符串值）
     */
    private Map<String, String> params;

    /**
     * 转为对外 JSON 结构（保持判别字段明确）
     *
     * @return 有序 Map
     */
    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        if (type == null) {
            return view;
        }
        view.put("type", type.name());
        switch (type) {
            case ARTICLE -> {
                view.put("articleId", refId);
                view.put("revisionId", revisionId);
            }
            case TOPIC -> {
                view.put("topicId", refId);
                view.put("revisionId", revisionId);
            }
            case H5 -> view.put("url", url);
            case NATIVE -> {
                view.put("routeCode", routeCode);
                view.put("params", params == null ? Map.of() : new LinkedHashMap<>(params));
            }
            default -> {
                // 枚举已覆盖全部分支
            }
        }
        return view;
    }
}
