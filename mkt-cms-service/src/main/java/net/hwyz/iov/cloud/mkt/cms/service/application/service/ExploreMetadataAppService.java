package net.hwyz.iov.cloud.mkt.cms.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreComponentType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExplorePublishType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreSourceType;
import net.hwyz.iov.cloud.mkt.cms.service.common.enums.ExploreTargetType;
import net.hwyz.iov.cloud.mkt.cms.service.infrastructure.config.ExploreProperties;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 探索页管理元数据应用服务。
 * <p>
 * MKT-CMS-DSN-CR-003 §6.1：下发组件、来源/目标类型、资源白名单、routeCode、约束和契约版本；
 * 管理端不得硬编码白名单或取值范围。
 *
 * @author hwyz_leo
 */
@Service
@RequiredArgsConstructor
public class ExploreMetadataAppService {

    private final ExploreProperties properties;

    /**
     * 元数据
     *
     * @return 元数据视图
     */
    public Map<String, Object> metadata() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("schemaVersion", 1);
        view.put("componentTypes", names(ExploreComponentType.values()));
        view.put("sourceTypes", names(ExploreSourceType.values()));
        view.put("targetTypes", names(ExploreTargetType.values()));
        view.put("publishTypes", names(ExplorePublishType.values()));
        view.put("assetHosts", sorted(properties.getAllowedAssetHosts()));
        view.put("h5Origins", sorted(properties.getAllowedH5Origins()));
        view.put("nativeRouteCodes", sorted(properties.getNativeRouteCodes()));
        view.put("nativeParamKeys", sorted(properties.getNativeParamKeys()));
        view.put("imageMimeTypes", List.of("image/jpeg", "image/png", "image/webp"));
        view.put("topicStyles", List.of("ADAPTIVE_GRID", "HORIZONTAL_SCROLL", "IMAGE_TEXT_LIST"));
        view.put("articleImageLayouts", List.of("SINGLE", "TWO_COLUMNS", "ONE_LARGE_TWO_SMALL",
                "TWO_BY_TWO", "THREE_COLUMNS", "NINE_GRID_WITH_COUNT"));

        Map<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("maxTitleLength", 255);
        constraints.put("maxSummaryLength", 1024);
        constraints.put("maxPageComponents", 20);
        constraints.put("maxArticleImages", 9);
        constraints.put("topicColumns", List.of(2, 3, 4));
        constraints.put("feedPageSizeDefault", properties.getFeedPageSizeDefault());
        constraints.put("feedPageSizeMax", properties.getFeedPageSizeMax());
        constraints.put("itemTimeRule", "endAt 必须晚于 startAt；endAt 不含");
        view.put("constraints", constraints);
        return view;
    }

    private List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toList());
    }

    private List<String> sorted(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }
}
