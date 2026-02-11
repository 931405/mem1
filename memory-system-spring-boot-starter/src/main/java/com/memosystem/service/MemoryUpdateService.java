package com.memosystem.service;

import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.vo.TokenUsageVO;

import java.util.List;

/**
 * 记忆更新服务接口
 * 负责决策和执行记忆库的增删改操作
 */
public interface MemoryUpdateService {
    /**
     * 根据候选记忆列表更新记忆库
     * 系统会自动决定是添加、更新还是删除记忆
     * @param sessionId 会话 ID
     * @param candidateMemories 候选记忆列表
     */
    void updateMemories(String sessionId, List<CandidateMemory> candidateMemories);

    /**
     * 根据候选记忆列表更新记忆库，同时返回 token 用量统计
     * @param sessionId 会话 ID
     * @param candidateMemories 候选记忆列表
     * @return 每次 LLM 决策调用的 token 用量列表
     */
    List<TokenUsageVO> updateMemoriesWithUsage(String sessionId, List<CandidateMemory> candidateMemories);

    /**
     * 添加新的记忆到记忆库
     * @param sessionId 会话 ID
     * @param candidateMemory 要添加的候选记忆
     */
    void addMemory(String sessionId, CandidateMemory candidateMemory);

    /**
     * 更新现有的记忆
     * @param sessionId 会话 ID
     * @param memoryId 记忆 ID
     * @param candidateMemory 要更新的候选记忆
     */
    void updateMemory(String sessionId, String memoryId, CandidateMemory candidateMemory);

    /**
     * 删除记忆
     * @param sessionId 会话 ID
     * @param memoryId 要删除的记忆 ID
     */
    void deleteMemory(String sessionId, String memoryId);

    /**
     * 获取记忆库统计信息
     * @return 统计信息字符串
     */
    String getMemoryStatistics();
}