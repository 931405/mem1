package com.memosystem.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 计时切面
 * 用于记录方法执行时间，辅助性能监控和优化
 */
@Aspect
@Component("mem0TimingAspect")
@Slf4j
public class TimingAspect {

    @Around("@annotation(timingMonitor)")
    public Object monitorTiming(ProceedingJoinPoint joinPoint, TimingMonitor timingMonitor) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String operationName = timingMonitor.value().isEmpty() ? methodName : timingMonitor.value();

        Long startTime = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            Long endTime = System.currentTimeMillis();
            Long duration = endTime - startTime;
            if (timingMonitor.verbose()) {
                log.info("方法[{}]执行完成，耗时：{} ms", operationName, duration);
            }
        }
    }
}