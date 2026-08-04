package com.example.snowydemo.webapp.config;

import com.example.snowydemo.webapp.interceptor.DemoTraceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把 DemoTraceInterceptor 注册到 Spring MVC 请求链。
 *
 * <p>{@code @Component} 只负责把拦截器对象交给 Spring 管理；addInterceptors 才表示把这个对象接入
 * MVC 的执行链。它只匹配 /api/**，并使用最高优先级，因此会先于 AuthConfig 中默认顺序的
 * SaInterceptor 执行。这样即使 Sa-Token 拒绝请求，X-Handler 已经写入响应，并且本拦截器仍能收到
 * afterCompletion 回调。</p>
 */
// @Configuration 让 Spring 在启动时读取本类中的 MVC 配置。
@Configuration
// 实现 WebMvcConfigurer 后，可以通过回调方法向 Spring MVC 添加拦截器等自定义配置。
public class RequestPipelineConfig implements WebMvcConfigurer {

    // 保存 Spring 注入的拦截器 Bean，后面把同一个对象注册到 MVC 请求链中。
    private final DemoTraceInterceptor demoTraceInterceptor;

    // 构造器注入：Spring 创建本配置类时，从容器中找到 DemoTraceInterceptor 并传进来。
    public RequestPipelineConfig(DemoTraceInterceptor demoTraceInterceptor) {
        // 保存依赖；final 保证构造完成后引用不会被替换。
        this.demoTraceInterceptor = demoTraceInterceptor;
    }

    // @Override 表示实现 WebMvcConfigurer 约定的拦截器注册方法。
    @Override
    // Spring MVC 初始化时调用此方法，registry 相当于“拦截器登记表”。
    public void addInterceptors(InterceptorRegistry registry) {
        // 把 DemoTraceInterceptor 接入 DispatcherServlet 管理的 Handler 执行链。
        registry.addInterceptor(demoTraceInterceptor)
                // 只拦截 /api 开头的接口，不处理其他静态资源路径。
                .addPathPatterns("/api/**")
                // 同类拦截器中优先执行它的 preHandle；响应返回时则按相反方向退出。
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
