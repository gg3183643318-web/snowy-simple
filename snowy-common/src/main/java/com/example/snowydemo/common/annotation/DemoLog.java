package com.example.snowydemo.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要展示 Service 调用日志的方法。
 *
 * <p>这个注解位于 common 层，只提供跨模块都能使用的元数据，本身不会执行日志逻辑。
 * web-app 层的 Spring AOP 切面会在运行期读取它，在目标方法前后记录同一次请求的调用顺序；
 * 因此必须使用 {@link RetentionPolicy#RUNTIME}，并且只允许标记方法。{@link #value()}
 * 用来描述当前业务动作，便于日志直接表达“正在执行什么”。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DemoLog {

    String value();
}
