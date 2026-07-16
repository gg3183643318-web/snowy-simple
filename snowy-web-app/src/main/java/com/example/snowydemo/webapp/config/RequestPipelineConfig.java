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
@Configuration
public class RequestPipelineConfig implements WebMvcConfigurer {

    private final DemoTraceInterceptor demoTraceInterceptor;

    public RequestPipelineConfig(DemoTraceInterceptor demoTraceInterceptor) {
        this.demoTraceInterceptor = demoTraceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoTraceInterceptor)
                .addPathPatterns("/api/**")
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
