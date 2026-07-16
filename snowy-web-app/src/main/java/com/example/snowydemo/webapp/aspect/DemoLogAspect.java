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
@Aspect
@Component
public class DemoLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoLogAspect.class);

    @Around("@annotation(demoLog)")
    public Object logDemoOperation(ProceedingJoinPoint joinPoint, DemoLog demoLog) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "#" + joinPoint.getSignature().getName();
        String traceId = TraceContext.get();
        LOGGER.info("[{}] 3 AOP before: {} ({})", traceId, demoLog.value(), methodName);
        try {
            return joinPoint.proceed();
        } finally {
            LOGGER.info("[{}] 4 AOP after: {} ({})", traceId, demoLog.value(), methodName);
        }
    }
}
