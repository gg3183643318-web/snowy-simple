package com.example.snowydemo.plugin.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 把 Sa-Token 的登录与权限能力接入 Spring MVC。
 *
 * <p>Spring MVC 收到请求后，HandlerMapping 会先根据 URL、HTTP 方法等信息选中具体的
 * HandlerMethod，也就是将要执行的 Controller 方法。选中之后、真正调用 Controller 之前，
 * {@link SaInterceptor} 才执行注解检查，因此它能够读取方法上的登录或权限注解，并决定请求是继续
 * 进入 Controller，还是直接抛出认证/授权异常。</p>
 *
 * <p>{@link StpInterface} 可以理解为 Sa-Token 向业务代码发起的查询接口：框架只知道当前 loginId，
 * 不知道这个用户在 CRM 业务里拥有哪些权限和角色，所以在检查权限注解时会回调这里，由业务代码
 * 返回权限码与角色列表。本示例把这些信息固定在内存中，便于观察调用链。</p>
 *
 * <p>当前还使用 Sa-Token 默认的内存数据存储，只适合教学演示。它没有 Redis 或数据库持久化，
 * 应用一旦重启，所有登录 token 和会话都会清空，不能直接当作生产环境的登录方案。</p>
 */
@Configuration
public class AuthConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public StpInterface stpInterface() {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                long userId = toUserId(loginId);
                if (userId == 1L) {
                    return List.of(
                            "crm:customer:read",
                            "crm:customer:create",
                            "crm:customer:update",
                            "crm:customer:delete"
                    );
                }
                if (userId == 2L) {
                    return List.of("crm:customer:read");
                }
                return List.of();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                long userId = toUserId(loginId);
                if (userId == 1L) {
                    return List.of("admin");
                }
                if (userId == 2L) {
                    return List.of("viewer");
                }
                return List.of();
            }

            private long toUserId(Object loginId) {
                try {
                    return Long.parseLong(String.valueOf(loginId));
                } catch (NumberFormatException exception) {
                    return -1L;
                }
            }
        };
    }
}
