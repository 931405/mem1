package com.memosystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置
 * 支持通过配置文件自定义参数
 */
@Configuration("mem0ThreadPoolConfig")
@Slf4j
public class ThreadPoolConfig {

    @Autowired
    private MemorySystemProperties properties;

    @Bean(name = "mem0ThreadPoolExecutor")
    public ThreadPoolExecutor myThreadPoolExecutor() {
        MemorySystemProperties.ThreadPool config = properties.getThreadPool();

        int corePoolSize = config.getCoreSize();
        int maxPoolSize = config.getMaxSize();
        long keepAliveTime = config.getKeepAliveSeconds();
        int queueCapacity = config.getQueueCapacity();

        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueCapacity);

        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("memory-pool-" + counter.getAndIncrement());
                t.setDaemon(false); // 非守护线程，确保任务执行完成
                return t;
            }
        };

        // 任务拒绝处理策略：如果队列满，主线程执行任务
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                unit,
                queue,
                threadFactory,
                rejectionHandler);

        log.info("线程池初始化完成：核心线程数={}, 最大线程数={}, 队列容量={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }
}