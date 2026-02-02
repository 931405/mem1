package com.memosystem.service.impl;

import com.memosystem.adapter.storage.QdrantLocalClient;
import com.memosystem.core.conversation.MessagePair;
import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.service.EmbeddingService;
import com.memosystem.service.MemoryUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 记忆更新服务实现
 * 负责决策和执行记忆库的增删改操作
 */
@Service("mem0MemoryUpdateService")
@Slf4j
public class MemoryUpdateServiceImpl implements MemoryUpdateService {

    private final AtomicInteger addedCount = new AtomicInteger(0);
    private final AtomicInteger updatedCount = new AtomicInteger(0);
    private final AtomicInteger deletedCount = new AtomicInteger(0);

    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private QdrantLocalClient qdrantClient;
    @Autowired
    private MessageUpdateStage messageUpdateStage;
    @Autowired
    @Qualifier("mem0ThreadPoolExecutor")
    private ThreadPoolExecutor executor;

    /**
     * 根据候选记忆列表并行更新记忆库
     * 使用自定义线程池实现并行处理
     * 
     * @param sessionId         会话ID
     * @param candidateMemories 候选记忆列表
     */
    @Override
    public void updateMemories(String sessionId, List<CandidateMemory> candidateMemories) {
        if (candidateMemories == null || candidateMemories.isEmpty()) {
            log.debug("没有候选记忆需要处理");
            return;
        }

        log.debug("========== 开始并行处理 {} 个候选记忆 ==========", candidateMemories.size());

        // 使用Map记录各候选记忆的耗时
        Map<Integer, Long> timings = new LinkedHashMap<>();
        long totalStartTime = System.currentTimeMillis();
        Map<Integer, String> memoryFacts = new LinkedHashMap<>();

        try {
            List<CompletableFuture<Void>> tasks = candidateMemories.stream()
                    .map((memory) -> {
                        int index = candidateMemories.indexOf(memory);
                        memoryFacts.put(index, memory.getFact());

                        return CompletableFuture.runAsync(() -> {
                            long candidateStartTime = System.currentTimeMillis();
                            try {
                                log.debug("【线程池处理 {}/{}】线程：{}，事实：{}",
                                        index + 1, candidateMemories.size(),
                                        Thread.currentThread().getName(),
                                        memory.getFact());

                                messageUpdateStage.processSingleCandidate(sessionId, memory);

                                long candidateDuration = System.currentTimeMillis() - candidateStartTime;
                                timings.put(index, candidateDuration);
                                log.debug("候选记忆 {}/{} 处理完成，耗时：{}ms",
                                        index + 1, candidateMemories.size(), candidateDuration);
                            } catch (Exception e) {
                                long candidateDuration = System.currentTimeMillis() - candidateStartTime;
                                timings.put(index, candidateDuration);
                                log.error("候选记忆 {}/{} 处理异常，耗时：{}ms，事实：{}",
                                        index + 1, candidateMemories.size(), candidateDuration, memory.getFact(), e);
                            }
                        }, executor);
                    }).collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
            long totalDuration = System.currentTimeMillis() - totalStartTime;

        } catch (Exception e) {
            log.error("并行处理候选记忆异常", e);
        }
    }

    @Override
    public void addMemory(String sessionId, CandidateMemory candidateMemory) {
        log.info("添加新记忆 - 事实: {}", candidateMemory.getFact());

        try {
            // 生成向量表示
            String text = candidateMemory.getFact() + " " + candidateMemory.getCategory();
            List<Float> embedding = embeddingService.embed(text);

            // 创建一个关联的消息对（这里使用虚拟的消息对，实际可能需要从真实对话中获取）
            MessagePair messagePair = new MessagePair(
                    sessionId,
                    "User fact: " + candidateMemory.getFact(),
                    "AI extracted: " + candidateMemory.getCategory(),
                    System.currentTimeMillis());

            // 调用向量数据库添加记忆
            qdrantClient.upsertMemory(sessionId, messagePair, embedding, candidateMemory);

            addedCount.incrementAndGet();
            log.info("记忆添加成功 - 事实: {}", candidateMemory.getFact());

        } catch (Exception e) {
            log.error("添加记忆失败 - 事实: {}", candidateMemory.getFact(), e);
        }
    }

    @Override
    public void updateMemory(String sessionId, String memoryId, CandidateMemory candidateMemory) {
        log.info("更新现有记忆 - ID: {}, 事实: {}", memoryId, candidateMemory.getFact());

        try {
            // 生成新的向量表示
            String text = candidateMemory.getFact() + " " + candidateMemory.getCategory();
            List<Float> newEmbedding = embeddingService.embed(text);

            // 调用向量数据库更新记忆，直接使用提供的 sessionId
            qdrantClient.updateMemory(sessionId, memoryId, newEmbedding, candidateMemory);
            updatedCount.incrementAndGet();
            log.info("记忆更新成功 - ID: {}, 事实: {}", memoryId, candidateMemory.getFact());

        } catch (Exception e) {
            log.error("更新记忆失败 - ID: {}", memoryId, e);
        }
    }

    @Override
    public void deleteMemory(String sessionId, String memoryId) {
        log.info("删除记忆 - sessionId: {}, ID: {}", sessionId, memoryId);

        try {
            // 调用向量数据库删除记忆
            qdrantClient.deleteMemory(memoryId);

            deletedCount.incrementAndGet();
            log.info("记忆删除成功 - sessionId: {}, ID: {}", sessionId, memoryId);

        } catch (Exception e) {
            log.error("删除记忆失败 - sessionId: {}, ID: {}", sessionId, memoryId, e);
        }
    }

    @Override
    public String getMemoryStatistics() {
        return String.format("新增: %d, 更新: %d, 删除: %d",
                addedCount.get(),
                updatedCount.get(),
                deletedCount.get());
    }
}