package com.memosystem.adapter.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.memosystem.config.MemorySystemProperties;
import com.memosystem.core.conversation.MessagePair;
import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.core.memory.MemoryEntry;
import com.memosystem.core.memory.MemorySimilarity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 本地Qdrant向量数据库客户端
 * 使用本地文件存储向量和记忆数据
 */
@Component("mem0QdrantLocalClient")
@Slf4j
public class QdrantLocalClient {

    @Autowired
    private MemorySystemProperties memoryConfig;

    private Gson gson = new Gson();
    private Map<String, MemoryEntry> memoriesMap = new LinkedHashMap<>();

    /**
     * 初始化数据库，创建目录结构
     */
    @PostConstruct
    private void initializeDatabase() {
        try {
            // 只创建目录，不删除已有数据
            Path dbPath = Paths.get(memoryConfig.getDbPath());
            if (!Files.exists(dbPath)) {
                Files.createDirectories(dbPath);
                log.info("已创建Qdrant数据库目录：" + memoryConfig.getDbPath());
            }

            Path collectionsPath = Paths.get(memoryConfig.getCollectionsPath());
            if (!Files.exists(collectionsPath)) {
                Files.createDirectories(collectionsPath);
                log.info("已创建集合目录：" + memoryConfig.getCollectionsPath());
            }

            // 只初始化不存在的集合文件
            Path memoriesFile = Paths.get(memoryConfig.getMemoriesFilePath());
            if (!Files.exists(memoriesFile)) {
                Files.write(memoriesFile, "[]".getBytes(StandardCharsets.UTF_8));
                log.info("已初始化记忆集合文件：" + memoryConfig.getMemoriesFilePath());
            } else {
                log.info("记忆集合文件已存在，跳过初始化");
            }
        } catch (IOException e) {
            log.error("初始化数据库失败", e);
            throw new RuntimeException("无法初始化Qdrant数据库", e);
        }
    }

    /**
     * 添加或更新记忆（保留向后兼容性）
     */
    public void upsertMemory(String sessionId, MessagePair messagePair, List<Float> embedding,
            CandidateMemory candidateMemory) {
        try {
            loadMemoriesFromFile();
            String memoryId = UUID.randomUUID().toString();
            MemoryEntry entry = new MemoryEntry(memoryId, sessionId, messagePair, embedding, candidateMemory);
            memoriesMap.put(memoryId, entry);
            saveMemoriesToFile();
            log.info("已添加/更新记忆：" + memoryId);
        } catch (IOException e) {
            log.error("添加/更新记忆失败", e);
            throw new RuntimeException("无法添加/更新记忆", e);
        }
    }

    /**
     * 更新现有记忆：保持原有的 MessagePair，只更新向量和 CandidateMemory
     */
    public void updateMemory(String sessionId, String memoryId, List<Float> newEmbedding,
            CandidateMemory newCandidateMemory) {
        try {
            loadMemoriesFromFile();
            MemoryEntry existingEntry = memoriesMap.get(memoryId);
            if (existingEntry != null) {
                // 保持原有的 MessagePair，只更新向量和 CandidateMemory
                MemoryEntry updatedEntry = new MemoryEntry(
                        memoryId,
                        sessionId,
                        existingEntry.getMessagePair(), // 保持原有的消息对
                        newEmbedding, // 更新向量
                        newCandidateMemory // 更新候选记忆
                );
                memoriesMap.put(memoryId, updatedEntry);
                saveMemoriesToFile();
                log.info("已更新记忆：" + memoryId);
            } else {
                log.warn("找不到要更新的记忆：" + memoryId);
            }
        } catch (IOException e) {
            log.error("更新记忆失败", e);
            throw new RuntimeException("无法更新记忆", e);
        }
    }

    /**
     * 根据ID删除记忆
     */
    public void deleteMemory(String memoryId) {
        try {
            loadMemoriesFromFile();
            if (memoriesMap.remove(memoryId) != null) {
                saveMemoriesToFile();
                log.info("已删除记忆：" + memoryId);
            }
        } catch (IOException e) {
            log.error("删除记忆失败", e);
            throw new RuntimeException("无法删除记忆", e);
        }
    }

    /**
     * 获取所有记忆
     */
    public List<MemoryEntry> getAllMemories() {
        try {
            loadMemoriesFromFile();
            return new ArrayList<>(memoriesMap.values());
        } catch (IOException e) {
            log.error("获取所有记忆失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据ID获取记忆
     */
    public MemoryEntry getMemoryById(String memoryId) {
        try {
            loadMemoriesFromFile();
            return memoriesMap.get(memoryId);
        } catch (IOException e) {
            log.error("获取记忆失败", e);
            return null;
        }
    }

    /**
     * 搜索相似记忆（基于向量相似度）
     * 只在指定会话的记忆中搜索，返回 top k 最相似的记忆
     */
    public List<MemorySimilarity> searchSimilarMemories(String sessionId, List<Float> queryEmbedding, int topK) {
        try {
            loadMemoriesFromFile();

            // 只过滤该会话的记忆
            List<MemoryEntry> sessionMemories = memoriesMap.values().stream()
                    .filter(memory -> sessionId.equals(memory.getSessionId()))
                    .toList();

            if (sessionMemories.isEmpty()) {
                return new ArrayList<>();
            }

            // 计算所有记忆与查询向量的相似度
            List<MemorySimilarity> similarities = new ArrayList<>();
            for (MemoryEntry memory : sessionMemories) {
                float similarity = cosineSimilarity(queryEmbedding, memory.getEmbedding());
                similarities.add(new MemorySimilarity(
                        memory.getId(),
                        memory.getCandidateMemory(),
                        memory.getMessagePair(),
                        similarity));
            }

            // 按相似度排序,返回前 top k 个
            return similarities.stream()
                    .sorted(Comparator.comparingDouble(MemorySimilarity::getSimilarityScore).reversed())
                    .limit(topK)
                    .toList();
        } catch (IOException e) {
            log.error("搜索相似记忆失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索相似记忆并返回相似度
     * 只在指定会话的记忆中搜索
     */
    public List<MemorySimilarity> searchSimilarMemoriesWithScore(String sessionId, List<Float> queryEmbedding,
            int topK) {
        try {
            loadMemoriesFromFile();

            // 只过滤该会话的记忆
            List<MemoryEntry> sessionMemories = memoriesMap.values().stream()
                    .filter(memory -> sessionId.equals(memory.getSessionId()))
                    .toList();

            if (sessionMemories.isEmpty()) {
                return new ArrayList<>();
            }

            // 计算所有记忆与查询向量的相似度
            List<MemorySimilarity> similarities = new ArrayList<>();
            for (MemoryEntry memory : sessionMemories) {
                float similarity = cosineSimilarity(queryEmbedding, memory.getEmbedding());
                similarities.add(new MemorySimilarity(
                        memory.getId(),
                        memory.getCandidateMemory(),
                        memory.getMessagePair(),
                        similarity));
            }

            // 按相似度排序
            similarities.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

            // 返回top k
            List<MemorySimilarity> result = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, similarities.size()); i++) {
                result.add(similarities.get(i));
            }

            return result;
        } catch (IOException e) {
            log.error("搜索相似记忆失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 余弦相似度计算
     */
    private float cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1 == null || vec2 == null || vec1.size() != vec2.size()) {
            return 0f;
        }

        float dotProduct = 0f;
        float norm1 = 0f;
        float norm2 = 0f;

        for (int i = 0; i < vec1.size(); i++) {
            float v1 = vec1.get(i);
            float v2 = vec2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        norm1 = (float) Math.sqrt(norm1);
        norm2 = (float) Math.sqrt(norm2);

        if (norm1 == 0f || norm2 == 0f) {
            return 0f;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * 加载记忆从文件
     */
    private synchronized void loadMemoriesFromFile() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(memoryConfig.getMemoriesFilePath())),
                StandardCharsets.UTF_8);
        if (content.trim().isEmpty() || "[]".equals(content.trim())) {
            memoriesMap.clear();
        } else {
            List<MemoryEntry> memories = gson.fromJson(content, new TypeToken<List<MemoryEntry>>() {
            }.getType());
            memoriesMap.clear();
            for (MemoryEntry memory : memories) {
                memoriesMap.put(memory.getId(), memory);
            }
        }
    }

    /**
     * 保存记忆到文件
     */
    private synchronized void saveMemoriesToFile() throws IOException {
        List<MemoryEntry> memories = new ArrayList<>(memoriesMap.values());
        String json = gson.toJson(memories);
        Files.write(Paths.get(memoryConfig.getMemoriesFilePath()), json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取记忆总数
     */
    public int getMemoryCount() {
        try {
            loadMemoriesFromFile();
            return memoriesMap.size();
        } catch (IOException e) {
            log.error("获取记忆总数失败", e);
            return 0;
        }
    }

}
