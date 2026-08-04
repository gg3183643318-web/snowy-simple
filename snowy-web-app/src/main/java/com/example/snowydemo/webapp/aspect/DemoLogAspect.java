package com.example.snowydemo.webapp.aspect;

import com.example.snowydemo.common.annotation.DemoLog;
import com.example.snowydemo.webapp.trace.TraceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 为标有 {@link DemoLog} 的 Spring Bean 方法记录业务调用日志。
 *
 * <p>AOP 包裹的是 Spring Bean 方法，不依赖 Controller 或 HTTP，因此同一个 Service 从定时任务、
 * 消息消费等入口调用时也可以进入该切面。它与 Filter、Interceptor 所处层次不同：前两者观察请求链，
 * AOP 观察具体方法调用。{@code @DemoLog} 本身只是被动元数据，不会主动打印日志；必须由此切面读取
 * 注解并在 {@code proceed()} 前后执行逻辑。</p>
 */
// @Aspect 告诉 Spring AOP：这个类中定义了切点和通知，不是普通业务 Service。
@Aspect
// @Component 让 Spring 创建该切面 Bean；只有 @Aspect 而没有交给 Spring 管理时，切面不会生效。
@Component
public class DemoLogAspect {

    // 创建当前切面使用的日志记录器。
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoLogAspect.class);

    // @Around 表示环绕通知：凡是调用带 @DemoLog 的 Spring Bean 方法，都会先进入本方法。
    // 表达式中的 demoLog 会把目标方法上的注解对象传给下面的同名参数。
    @Around("@annotation(demoLog)")
    // joinPoint 代表“本来准备执行的目标方法”；返回 Object 是为了兼容不同方法的返回类型。
    public Object logDemoOperation(ProceedingJoinPoint joinPoint, DemoLog demoLog) throws Throwable {
        // 从连接点读取目标类和方法名，例如 CrmCustomerService#listCustomers。
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "#" + joinPoint.getSignature().getName();
        // 取得 Filter 在当前请求线程中保存的 traceId，让 AOP 日志能和 HTTP 请求日志对应。
        String traceId = TraceContext.get();
        // 在业务 Service 方法真正执行之前打印第 3 步日志。
        LOGGER.info("[{}] 3 AOP before: {} ({})", traceId, demoLog.value(), methodName);
        // finally 保证目标方法正常返回或抛异常时，after 日志都能执行。
        try {
            // 真正调用原来的 Service 方法；没有 proceed()，目标业务方法就不会执行。
            return joinPoint.proceed();
        } finally {
            // 目标方法已经返回或抛出异常，打印第 4 步日志，然后继续把结果或异常交回调用方。
            LOGGER.info("[{}] 4 AOP after: {} ({})", traceId, demoLog.value(), methodName);
        }
    }
}
