package com.memosystem.adapter.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import lombok.Data;
import lombok.Getter;

/**
 * LLM 统一配置类
 * 集中管理所有 LLM 相关的配置信息和 Bean 定义
 * 所有配置从 application.yaml 中读取
 */
@Configuration("mem0LLMConfig")
@EnableConfigurationProperties(LLMConfig.LLMProperties.class)
public class LLMConfig {

    @Getter
    private final LLMProperties llmProperties;

    public LLMConfig(LLMProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    /**
     * LLM 配置属性类
     */
    @Data
    @ConfigurationProperties(prefix = "llm")
    public static class LLMProperties {
        private String apiKey;
        private String apiUrl = "https://one-api.maas.com.cn/v1";
        private String defaultModel = "deepseek-v3-2-251201";
        private String memoryExtractionModel = "deepseek-v3-2-251201";
        private String decisionModel = "deepseek-v3-2-251201";
        private String globalMemoryModel = "deepseek-v3-2-251201";
        private Integer connectTimeout = 10;
        private Integer apiTimeout = 30;
        private Double chatTemperature = 0.7;
        private Double memoryExtractionTemperature = 0.0;
        private Double decisionTemperature = 0.0;
        private Integer maxTokens = 2000;
    }

    // ============ 实例方法 ============

    public String getApiKey() {
        String apiKey = llmProperties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("未配置 API Key！请在 application.yaml 中配置：llm.api-key");
        }
        return apiKey;
    }

    public String getApiBaseUrl() {
        return llmProperties.getApiUrl();
    }

    public String getChatCompletionEndpoint() {
        return getApiBaseUrl() + "/chat/completions";
    }

    public String getDefaultModel() {
        return llmProperties.getDefaultModel();
    }

    public String getMemoryExtractionModel() {
        return llmProperties.getMemoryExtractionModel();
    }

    public String getDecisionModel() {
        return llmProperties.getDecisionModel();
    }

    public String getGlobalMemoryModel() {
        return llmProperties.getGlobalMemoryModel();
    }

    public int getConnectTimeout() {
        return llmProperties.getConnectTimeout();
    }

    public int getApiTimeout() {
        return llmProperties.getApiTimeout();
    }

    public double getChatTemperature() {
        return llmProperties.getChatTemperature();
    }

    public double getMemoryExtractionTemperature() {
        return llmProperties.getMemoryExtractionTemperature();
    }

    public double getDecisionTemperature() {
        return llmProperties.getDecisionTemperature();
    }

    public int getMaxTokens() {
        return llmProperties.getMaxTokens();
    }

    // ============ Spring Bean 配置 ============

    // 默认 LLM 客户端 Bean
    @Bean(name = "mem0DefaultLLMClient")
    @Primary
    public LLMClient llmClient() {
        return new LLMClient(
                getApiKey(),
                getDefaultModel(),
                getChatCompletionEndpoint(),
                getConnectTimeout(),
                getApiTimeout(),
                getChatTemperature(),
                getMaxTokens());
    }

    // 专用 LLM 客户端 Bean - 记忆提取
    @Bean(name = "mem0MemoryExtractionClient")
    public LLMClient memoryExtractionClient() {
        return new LLMClient(
                getApiKey(),
                getMemoryExtractionModel(),
                getChatCompletionEndpoint(),
                getConnectTimeout(),
                getApiTimeout(),
                getMemoryExtractionTemperature(),
                getMaxTokens());
    }

    // 专用 LLM 客户端 Bean - 决策制定
    @Bean(name = "mem0DecisionLLMClient")
    public LLMClient decisionLLMClient() {
        return new LLMClient(
                getApiKey(),
                getDecisionModel(),
                getChatCompletionEndpoint(),
                getConnectTimeout(),
                getApiTimeout(),
                getDecisionTemperature(),
                getMaxTokens());
    }

    // 专用 LLM 客户端 Bean - 全局记忆
    @Bean(name = "mem0GlobalMemoryLLMClient")
    public LLMClient globalMemoryLLMClient() {
        return new LLMClient(
                getApiKey(),
                getGlobalMemoryModel(),
                getChatCompletionEndpoint(),
                getConnectTimeout(),
                getApiTimeout(),
                getChatTemperature(),
                getMaxTokens());
    }
}