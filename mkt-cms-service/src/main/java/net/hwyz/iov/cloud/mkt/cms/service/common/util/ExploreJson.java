package net.hwyz.iov.cloud.mkt.cms.service.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreErrorCode;
import net.hwyz.iov.cloud.mkt.cms.service.common.exception.ExploreException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 探索页 JSON 工具（展示快照、受控配置与规范化哈希）。
 * <p>
 * 固定“Map 键排序 + 不美化输出”，保证相同业务内容产生相同序列化结果与哈希。
 *
 * @author hwyz_leo
 */
public final class ExploreJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private ExploreJson() {
    }

    /**
     * 序列化为紧凑 JSON
     *
     * @param value 对象
     * @return JSON 字符串
     */
    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "JSON 序列化失败", e);
        }
    }

    /**
     * 解析为有序 Map
     *
     * @param json JSON 字符串
     * @return Map；空值返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> readMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "JSON 解析失败", e);
        }
    }

    /**
     * 解析为字符串 Map（受控参数）
     *
     * @param json JSON 字符串
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> readStringMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new ExploreException(ExploreErrorCode.EXPLORE_INVALID_ARGUMENT, "JSON 解析失败", e);
        }
    }
}
