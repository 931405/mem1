package com.memosystem.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.memosystem.adapter.storage.QdrantLocalClient;
import com.memosystem.common.model.ParsedMessage;
import com.memosystem.config.MemorySystemProperties;
import com.memosystem.core.conversation.MessagePair;
import com.memosystem.core.memory.MemorySimilarity;
import com.memosystem.service.EmbeddingService;
import com.memosystem.service.MemoryRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆检索服务实现
 */
@Service("mem0MemoryRetrieverService")
@Slf4j
public class MemoryRetrieverServiceImpl implements MemoryRetrieverService {

    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private QdrantLocalClient qdrantLocalClient;
    @Autowired
    private MemorySystemProperties memoryConfig;
    private static final Gson GSON = new Gson();

    /**
     * 检索与用户消息相似的记忆
     * 
     * @param sessionId
     * @param userMessage 用户输入的消息
     * @param topK        返回的前 K 个最相似的记忆
     * @return
     */
    @Override
    public List<MemorySimilarity> retrieveSimilarMemories(String sessionId, String userMessage, int topK) {

        try {
            // 获取消息的向量表示
            List<Float> queryEmbedding = embeddingService.embed(userMessage);

            // 从向量数据库中检索相似记忆
            List<MemorySimilarity> memorySimilarities = qdrantLocalClient.searchSimilarMemories(sessionId,
                    queryEmbedding, topK);
            log.debug("检索完成，找到 {} 条相似记忆", memorySimilarities.size());
            return memorySimilarities;

        } catch (Exception e) {
            log.error("记忆检索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 检索与解析消息相似的记忆
     * 
     * @param sessionId
     * @param parsedMessage 解析后的消息对象
     * @param topK          返回的前 K 个最相似的记忆
     * @return
     */
    @Override
    public List<MemorySimilarity> retrieveSimilarMemories(String sessionId, ParsedMessage parsedMessage, int topK) {
        log.debug("检索相似记忆 - 语言: {}, topK: {}", parsedMessage.getLanguage(), topK);
        return retrieveSimilarMemories(sessionId, parsedMessage.getContent(), topK);
    }

    /**
     * 获取最近的消息 Top N
     * 
     * @param sessionId
     * @param fileName  消息文件名
     * @return
     */
    @Override
    public String getRecentMemories(String sessionId, String fileName) {
        return getRecentMemories(sessionId, fileName, memoryConfig.getConversationSearchTopK());
    }

    /**
     * 获取最近的消息 Top N
     * 
     * @param sessionId 会话 ID
     * @param fileName  消息文件名
     * @param limit     消息数量限制
     * @return
     */
    @Override
    public String getRecentMemories(String sessionId, String fileName, int limit) {

        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            log.warn("消息文件不存在: {}", fileName);
            return "";
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<MessagePair> messages = GSON.fromJson(
                    reader,
                    new TypeToken<List<MessagePair>>() {
                    }.getType());

            if (messages == null || messages.isEmpty()) {
                return "";
            }
            // 过滤出指定 sessionId 的消息
            List<MessagePair> messagePairList = messages.stream().filter(msg -> msg.getSessionId().equals(sessionId))
                    .collect(Collectors.toCollection(ArrayList::new));
            // 按时间戳排序（从新到旧）
            messagePairList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

            // 取最近的 N 条
            StringBuilder result = new StringBuilder();
            int count = Math.min(limit, messagePairList.size());
            for (int i = 0; i < count; i++) {
                MessagePair msg = messagePairList.get(i);
                result.append(String.format("【消息 %d】\n", i + 1));
                result.append("用户: ").append(msg.getUserMessage()).append("\n");
                result.append("AI: ").append(msg.getAiResponse()).append("\n");
                result.append("时间: ").append(msg.getTimestamp()).append("\n");
                result.append("\n");
            }

            return result.toString();

        } catch (IOException e) {
            log.error("读取消息文件失败: {}", fileName, e);
            return "";
        }
    }
}