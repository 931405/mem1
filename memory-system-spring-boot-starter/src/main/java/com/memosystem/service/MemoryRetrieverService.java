package com.memosystem.service;

import com.memosystem.common.model.ParsedMessage;
import com.memosystem.core.memory.MemorySimilarity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记忆检索服务接口
 * 负责从向量数据库中检索相似的历史记忆
 */
@Service("mem0MemoryRetrieverServiceInterface")
public interface MemoryRetrieverService {
    /**
     * 根据用户消息检索相似的记忆
     * 
     * @param userMessage 用户输入的消息
     * @param topK        返回的前 K 个最相似的记忆
     * @return 相似度排序后的记忆列表
     */
    List<MemorySimilarity> retrieveSimilarMemories(String sessionId, String userMessage, int topK);

    /**
     * 根据解析后的消息检索相似的记忆
     * 
     * @param parsedMessage 解析后的消息对象
     * @param topK          返回的前 K 个最相似的记忆
     * @return 相似度排序后的记忆列表
     */
    List<MemorySimilarity> retrieveSimilarMemories(String sessionId, ParsedMessage parsedMessage, int topK);

    /**
     * 获取最近的对话消息
     * 
     * @param fileName 消息文件名
     * @return 最近的对话消息的文本格式
     */
    String getRecentMemories(String sessionId, String fileName);

    /**
     * 获取最近 N 条消息
     * 
     * @param sessionId 会话 ID
     * @param fileName  消息文件名
     * @param limit     消息数量限制
     * @return 最近 N 条消息的文本格式
     */
    String getRecentMemories(String sessionId, String fileName, int limit);
}