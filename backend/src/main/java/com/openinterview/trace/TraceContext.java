package com.openinterview.trace;

public final class TraceContext {
    private static final ThreadLocal<String> TRACE_LOCAL = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void setTraceId(String traceId) {
        TRACE_LOCAL.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_LOCAL.get();
    }

    public static void clear() {
        TRACE_LOCAL.remove();
    }
}
