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
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DemoRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoRequestFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        TraceContext.set(traceId);
        response.setHeader("X-Trace-Id", traceId);
        LOGGER.info("[{}] 1 Filter before: {} {}", traceId, request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            LOGGER.info("[{}] 6 Filter after: {}", traceId, response.getStatus());
            TraceContext.clear();
        }
    }
}
