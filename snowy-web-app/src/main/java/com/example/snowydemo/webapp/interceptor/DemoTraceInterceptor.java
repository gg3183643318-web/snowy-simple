package com.example.snowydemo.webapp.interceptor;

import com.example.snowydemo.webapp.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 展示 Spring MVC 已选中 Controller 后的拦截器阶段。
 *
 * <p>执行 preHandle 时，HandlerMapping 已经根据请求路径和 HTTP 方法找到了目标 Controller 方法，
 * 但该方法还没有真正执行。因此这里能拿到 HandlerMethod，读取 Controller 类型、方法名和方法注解；
 * 这也是 MVC Interceptor 与更外层 Servlet Filter 的关键区别。</p>
 */
@Component
public class DemoTraceInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoTraceInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            String handlerName = handlerMethod.getBeanType().getSimpleName()
                    + "#" + handlerMethod.getMethod().getName();
            response.setHeader("X-Handler", handlerName);
            LOGGER.info("[{}] 2 Interceptor before: {}", TraceContext.get(), handlerName);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        if (handler instanceof HandlerMethod handlerMethod) {
            String handlerName = handlerMethod.getBeanType().getSimpleName()
                    + "#" + handlerMethod.getMethod().getName();
            LOGGER.info("[{}] 5 Interceptor after: {}", TraceContext.get(), handlerName);
        }
    }
}
