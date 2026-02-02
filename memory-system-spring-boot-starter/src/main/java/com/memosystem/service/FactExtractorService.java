package com.memosystem.service;

import com.memosystem.core.memory.CandidateMemory;

import java.util.List;

/**
 * 事实抽取服务接口
 * 负责从对话中抽取候选的新记忆事实
 */
public interface FactExtractorService {
    /**
     * 从对话中抽取候选记忆
     * @param globalSummary 全局记忆摘要
     * @param recentMemories 最近的对话记忆
     * @param userMessage 用户的输入消息
     * @param aiResponse AI 的响应消息
     * @return 抽取得到的候选记忆列表
     */
    List<CandidateMemory> extractCandidateMemories(
            String globalSummary,
            String recentMemories,
            String userMessage,
            String aiResponse
    );

    /**
     * 抽取候选记忆，使用指定的 LLM 模型
     * @param globalSummary 全局记忆摘要
     * @param recentMemories 最近的对话记忆
     * @param userMessage 用户的输入消息
     * @param aiResponse AI 的响应消息
     * @param model 指定的 LLM 模型
     * @return 抽取得到的候选记忆列表
     */
    List<CandidateMemory> extractCandidateMemories(
            String globalSummary,
            String recentMemories,
            String userMessage,
            String aiResponse,
            String model
    );
}