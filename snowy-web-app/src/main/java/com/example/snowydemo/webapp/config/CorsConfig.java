package com.example.snowydemo.webapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置浏览器跨域访问规则。
 *
 * <p>当前前端运行在 {@code http://localhost:8091}，后端运行在 {@code http://localhost:8080}。
 * 协议、域名、端口任意一个不同就属于不同 Origin，浏览器会执行同源策略检查。这个配置只影响浏览器
 * 是否允许前端 JavaScript 读取后端响应，不是 Controller 的登录或权限验证。</p>
 */
// @Configuration 把本类标记为 Spring 配置类，应用启动时会创建并读取它。
@Configuration
// WebMvcConfigurer 提供扩展 Spring MVC 配置的回调方法，不需要我们替换 MVC 默认实现。
public class CorsConfig implements WebMvcConfigurer {

    // @Override 表示当前方法实现了 WebMvcConfigurer 中约定的同名方法。
    @Override
    // Spring Boot 启动配置 MVC 时调用此方法，并把用于登记跨域规则的 registry 传进来。
    public void addCorsMappings(CorsRegistry registry) {
        // "/**" 表示规则覆盖后端的所有请求路径；一个 * 只匹配一层，两个 * 可以匹配多层。
        registry.addMapping("/**")
                // 教学项目允许任意来源访问；生产环境通常应明确填写可信前端域名。
                .allowedOriginPatterns("*")
                // 浏览器跨域请求只允许使用这里列出的 HTTP 方法。
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许浏览器发送任意请求头，包括本项目保存 Sa-Token 的 token 请求头。
                .allowedHeaders("*")
                // 默认情况下 JS 不能读取所有自定义响应头；这里显式开放请求编号和 Controller 名称。
                .exposedHeaders("X-Trace-Id", "X-Handler");
    }
}
