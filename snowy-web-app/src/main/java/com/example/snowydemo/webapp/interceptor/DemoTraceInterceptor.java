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
// @Component 让 Spring 创建这个对象，RequestPipelineConfig 才能通过构造器取得并注册它。
@Component
// HandlerInterceptor 是 Spring MVC 的拦截器接口，工作位置在 DispatcherServlet 内部。
public class DemoTraceInterceptor implements HandlerInterceptor {

    // 创建当前类专用的日志记录器。
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoTraceInterceptor.class);

    // @Override 表示实现 Controller 执行前的拦截器回调。
    @Override
    // DispatcherServlet 已通过 HandlerMapping 找到处理器后、真正解析参数和调用 Controller 前执行此方法。
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // handler 也可能是静态资源处理器；只有 HandlerMethod 才代表一个 Controller 方法。
        if (handler instanceof HandlerMethod handlerMethod) {
            // 读取 Controller 类名和方法名，例如 AuthController#login。
            String handlerName = handlerMethod.getBeanType().getSimpleName()
                    + "#" + handlerMethod.getMethod().getName();
            // 把选中的 Controller 方法写入响应头，供浏览器请求实验室展示。
            response.setHeader("X-Handler", handlerName);
            // 此时只“找到了”Controller，方法本身尚未执行，所以记录 before 日志。
            LOGGER.info("[{}] 2 Interceptor before: {}", TraceContext.get(), handlerName);
        }
        // true 表示允许 DispatcherServlet 继续解析 @RequestBody、执行 @Valid 并调用 Controller。
        // 如果返回 false，请求链会在这里停止，Controller 不会执行。
        return true;
    }

    // @Override 表示实现本次 Handler 请求彻底结束后的回调。
    @Override
    // Controller 正常返回或请求处理阶段出现异常，Spring MVC 完成处理后都会进入这里。
    public void afterCompletion(
            // 本次 HTTP 请求对象。
            HttpServletRequest request,
            // 此时包含最终状态码和已经写入的响应头。
            HttpServletResponse response,
            // 与 preHandle 收到的是同一个目标处理器。
            Object handler,
            // 未被 Spring MVC 异常处理器消化的异常；正常完成时通常是 null。
            Exception exception) {
        // 只给真正的 Controller 方法记录 Handler 名称。
        if (handler instanceof HandlerMethod handlerMethod) {
            // 再次组合类名和方法名，便于确认响应是从哪个 Controller 返回的。
            String handlerName = handlerMethod.getBeanType().getSimpleName()
                    + "#" + handlerMethod.getMethod().getName();
            // after 日志说明 Controller、返回值转换和异常处理阶段已经结束。
            LOGGER.info("[{}] 5 Interceptor after: {}", TraceContext.get(), handlerName);
        }
    }
}
