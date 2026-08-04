package com.example.snowydemo.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 整个多模块项目的 Spring Boot 启动入口。
 *
 * <p>这个类位于 {@code com.example.snowydemo.webapp} 包。如果不指定扫描范围，Spring 默认只扫描
 * {@code webapp} 及其子包，无法找到与它平级的 {@code plugin.auth}、{@code plugin.crm} 等业务包。
 * 因此这里把扫描起点提升到 {@code com.example.snowydemo}，让各插件中的 Controller、Service 和配置类
 * 都能注册到同一个 Spring 容器。Maven 依赖负责让插件类出现在 classpath，组件扫描负责发现其中的 Bean，
 * 两个条件缺一不可。</p>
 */
// @SpringBootApplication 同时包含配置类、自动配置和组件扫描三项核心能力。
// scanBasePackages 明确告诉 Spring 从哪个 Java 包开始向下扫描组件。
@SpringBootApplication(scanBasePackages = "com.example.snowydemo")
public class SimpleSnowyDemoApplication {

    /**
     * JVM 执行 {@code java -jar} 时首先进入这个 main 方法。
     *
     * @param args 启动命令传入的参数，例如 {@code --server.port=9090}
     */
    public static void main(String[] args) {
        // 创建 Spring 容器、执行自动配置并启动内嵌 Tomcat；方法返回后应用会继续监听 HTTP 请求。
        SpringApplication.run(SimpleSnowyDemoApplication.class, args);
    }
}
