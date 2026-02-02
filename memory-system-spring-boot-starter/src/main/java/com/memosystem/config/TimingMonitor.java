package com.memosystem.config;


import java.lang.annotation.*;

/**
 * 方法执行时间监控注解
 * 用于标记需要监控执行时间的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TimingMonitor {

    /**
     * 操作名称，用于日志和指标记录
     * @return
     */
    String value() default "";

    /**
     * 是否启用详细日志记录
     * @return
     */
    boolean verbose() default true;
}
