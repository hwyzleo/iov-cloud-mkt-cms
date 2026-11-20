package net.hwyz.iov.cloud.mkt.cms.service.common.util;

import org.slf4j.MDC;

/**
 * 链路追踪上下文读取（仅用于操作留痕，不记录协议正文）。
 *
 * @author hwyz_leo
 */
public final class TraceContext {

    private static final String TRACE_ID = "traceId";

    private TraceContext() {
    }

    /**
     * 当前链路追踪 ID；不存在时返回 null。
     *
     * @return 链路追踪 ID
     */
    public static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID);
        return traceId == null || traceId.isEmpty() ? null : traceId;
    }
}
