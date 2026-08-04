package com.example.snowydemo.webapp.filter;

import com.example.snowydemo.webapp.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet 请求链最外层的演示 Filter。
 *
 * <p>Filter 运行在 DispatcherServlet 之前，此时 Spring MVC 还没有根据 URL 找到 Controller，
 * 所以它能观察到所有进入应用的请求，包括最终返回 404 的请求和浏览器发出的 OPTIONS 预检请求。
 * {@code filterChain.doFilter} 可以理解为把请求交给后面；后面的 Servlet、Interceptor、Controller
 * 全部处理完后，响应会沿原路返回到同一个 Filter，才执行 after 日志。</p>
 *
 * <p>traceId 放进 ThreadLocal 后，同一线程上的后续 Interceptor 和 AOP 都能读取。Tomcat 工作线程会
 * 被线程池反复复用，因此清理必须放在 finally：即使认证失败或业务抛异常，也不能把本次请求的数据
 * 留给下一次请求。</p>
 */
// @Component 让组件扫描发现本类，并把它创建为 Spring Bean。
@Component
// Filter 可以有多个；最高优先级使这个 Filter 尽早包住完整请求链。
@Order(Ordered.HIGHEST_PRECEDENCE)
// OncePerRequestFilter 保证一次请求分派中此 Filter 通常只执行一次，并让我们只需实现 doFilterInternal。
public class DemoRequestFilter extends OncePerRequestFilter {

    // 为当前类创建日志记录器，日志中会自动带上时间、线程和类名等信息。
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoRequestFilter.class);

    // @Override 表示覆盖 OncePerRequestFilter 提供的请求处理扩展点。
    @Override
    // Tomcat 先把原始 HTTP 数据整理成 request、response，再通过 Filter 链调用这里。
    protected void doFilterInternal(
            // request 包含请求方法、URI、请求头、请求体等客户端传来的信息。
            HttpServletRequest request,
            // response 用来设置状态码、响应头并最终向客户端写回响应数据。
            HttpServletResponse response,
            // filterChain 代表“当前 Filter 后面的组件”，其中最终包括 DispatcherServlet。
            FilterChain filterChain) throws ServletException, IOException {
        // 为每次请求创建一个短编号，方便把同一次请求经过不同层的日志串起来观察。
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        // 把编号绑定到当前 Tomcat 工作线程，让后面的 Interceptor 和 AOP 也能取得它。
        TraceContext.set(traceId);
        // 把编号写进 HTTP 响应头，前端可以用它和后端日志进行对应。
        response.setHeader("X-Trace-Id", traceId);
        // before 日志发生在 DispatcherServlet、Interceptor 和 Controller 之前。
        LOGGER.info("[{}] 1 Filter before: {} {}", traceId, request.getMethod(), request.getRequestURI());

        // try/finally 保证后面的代码正常返回或抛异常时，清理逻辑都会执行。
        try {
            /*
             * 把 request 和 response 继续交给后面的 Filter，最后到达 DispatcherServlet。
             * DispatcherServlet 随后使用 HandlerMapping 找 Controller，再执行 Interceptor 和 Controller。
             * 这一行会一直等待，直到整个后续请求链处理完并沿原路返回。
             * 如果不调用它，请求会停在当前 Filter，Controller 永远不会执行。
             */
            filterChain.doFilter(request, response);
        } finally {
            // 执行到这里时，后续请求链已经结束，所以可以读取最终 HTTP 状态码。
            LOGGER.info("[{}] 6 Filter after: {}", traceId, response.getStatus());
            // Tomcat 线程会重复使用，必须删除本次请求留下的 ThreadLocal 数据。
            TraceContext.clear();
        }
    }
}
