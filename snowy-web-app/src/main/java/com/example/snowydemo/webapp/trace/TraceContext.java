package com.example.snowydemo.webapp.trace;

/**
 * 保存当前请求的 traceId，让 Filter、Interceptor 和 AOP 能记录同一个请求标识。
 *
 * <p>Tomcat 使用线程池处理请求：一个工作线程处理完请求后不会销毁，而是回到线程池，之后还可能处理
 * 另一个用户的请求。ThreadLocal 中的数据跟线程绑定，因此如果请求结束时不清理，旧 traceId 就可能被
 * 复用该线程的下一个请求读到，既会串日志，也会一直占用相关对象。所以 Filter 必须在 finally 中调用
 * {@link #clear()}，无论 Controller 正常返回还是中途抛异常，都要完成清理。</p>
 */
// final 表示这是一个纯工具类，不允许通过继承改变它保存请求编号的规则。
public final class TraceContext {

    // 每个 Tomcat 工作线程都有自己独立的一份 String，多个并发请求不会直接覆盖彼此的 traceId。
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    // 私有构造器禁止 new TraceContext()；所有操作都通过下面的静态方法完成。
    private TraceContext() {
    }

    // Filter 在请求刚进入时调用，把本次请求编号保存到当前线程。
    public static void set(String traceId) {
        // set 只修改当前线程对应的值，不会写到其他请求线程中。
        TRACE_ID.set(traceId);
    }

    // Interceptor 和 AOP 在同一个请求线程中调用，取得 Filter 之前保存的编号。
    public static String get() {
        // 当前线程没有保存编号时返回 null，例如从非 HTTP 的定时任务直接调用 Service。
        return TRACE_ID.get();
    }

    // Filter 在 finally 中调用，确保请求结束后删除当前线程保存的数据。
    public static void clear() {
        // remove 比 set(null) 更明确地移除 ThreadLocal 条目，避免线程池复用导致数据残留。
        TRACE_ID.remove();
    }
}
