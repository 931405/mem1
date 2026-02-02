package com.memosystem.config;

import com.memosystem.service.EmbeddingService;
import com.memosystem.service.impl.DefaultEmbeddingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Memory System 自动配置类
 * 为 Spring Boot 应用自动配置所有必要的 Bean
 */
@Configuration("mem0MemorySystemAutoConfiguration")
@ComponentScan(basePackages = "com.memosystem")
@EnableConfigurationProperties(MemorySystemProperties.class)
public class MemorySystemAutoConfiguration {

    /**
     * 默认 EmbeddingService Bean
     * 如果使用方没有提供自定义实现，则使用此默认实现
     */
    @Bean("mem0EmbeddingService")
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService embeddingService() {
        return new DefaultEmbeddingService();
    }
}