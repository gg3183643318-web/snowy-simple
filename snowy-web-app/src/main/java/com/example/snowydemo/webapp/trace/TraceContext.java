package com.example.snowydemo.webapp.trace;

/**
 * 保存当前请求的 traceId，让 Filter、Interceptor 和 AOP 能记录同一个请求标识。
 *
 * <p>Tomcat 使用线程池处理请求：一个工作线程处理完请求后不会销毁，而是回到线程池，之后还可能处理
 * 另一个用户的请求。ThreadLocal 中的数据跟线程绑定，因此如果请求结束时不清理，旧 traceId 就可能被
 * 复用该线程的下一个请求读到，既会串日志，也会一直占用相关对象。所以 Filter 必须在 finally 中调用
 * {@link #clear()}，无论 Controller 正常返回还是中途抛异常，都要完成清理。</p>
 */
public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String get() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
