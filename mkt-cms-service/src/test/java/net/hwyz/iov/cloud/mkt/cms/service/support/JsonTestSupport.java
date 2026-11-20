package net.hwyz.iov.cloud.mkt.cms.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 测试用 JSON 序列化支持。
 * <p>
 * 与平台实际运行形态保持一致：注册 Java 时间模块并关闭时间戳输出，
 * 使 {@code LocalDate} 序列化为 {@code YYYY-MM-DD}，而不是数字数组。
 *
 * @author hwyz_leo
 */
public final class JsonTestSupport {

    private JsonTestSupport() {
    }

    /**
     * 构造与运行形态一致的 ObjectMapper
     *
     * @return ObjectMapper
     */
    public static ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
