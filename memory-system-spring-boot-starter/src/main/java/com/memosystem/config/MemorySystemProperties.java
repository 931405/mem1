package com.memosystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 记忆系统配置类
 * 统一管理系统中的各种常量配置
 */
@ConfigurationProperties(prefix = "memory")
@Data
public class MemorySystemProperties {

    /**
     * 对话服务中检索相似记忆的数量
     */
    private int conversationSearchTopK = 3;

    /**
     * 记忆更新阶段检索相似记忆的数量
     */
    private int updateSearchTopK = 5;

    /**
     * 短期记忆最大容量（每个会话保留的最近对话轮数）
     */
    private int shortTermMemorySize = 10;

    /**
     * Qdrant 向量数据库路径
     */
    private String dbPath = "./qdrant";

    /**
     * 集合存储目录
     */
    private String collectionsDir = "collections";

    /**
     * 记忆集合名称
     */
    private String collectionName = "memories";

    /**
     * API 配置
     */
    private Api api = new Api();

    /**
     * 线程池配置
     */
    private ThreadPool threadPool = new ThreadPool();

    /**
     * 获取集合完整路径
     */
    public String getCollectionsPath() {
        return dbPath + "/" + collectionsDir;
    }

    /**
     * 获取记忆文件完整路径
     */
    public String getMemoriesFilePath() {
        return getCollectionsPath() + "/" + collectionName + ".json";
    }

    /**
     * API 配置类
     */
    @Data
    public static class Api {
        /**
         * API 路径前缀
         */
        private String prefix = "/api/conversation";
    }

    /**
     * 线程池配置类
     */
    @Data
    public static class ThreadPool {
        /**
         * 核心线程数
         */
        private int coreSize = 5;

        /**
         * 最大线程数
         */
        private int maxSize = 10;

        /**
         * 线程空闲时间（秒）
         */
        private long keepAliveSeconds = 60;

        /**
         * 任务队列大小
         */
        private int queueCapacity = 100;
    }
}