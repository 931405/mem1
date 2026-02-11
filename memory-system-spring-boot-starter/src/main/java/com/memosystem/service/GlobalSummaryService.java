package com.memosystem.service;

import com.memosystem.vo.TokenUsageVO;

/**
 * 全局摘要服务接口
 * 负责生成和维护用户的全局记忆摘要
 */
public interface GlobalSummaryService {
    /**
     * 生成执行历史的摘要（Procedural Memory）
     * 包含用户的关键属性和长期记忆
     * @return 全局摘要文本
     */
//    String generateProcedureMemory();

    /**
     * 使用指定模型生成执行历史的摘要
     * @param model 指定的 LLM 模型
     * @return 全局摘要文本
     */
//    String generateProcedureMemory(String model);

    /**
     * 更新全局摘要
     * 基于最后的消息对和 PROCEDURAL_MEMORY_SYSTEM_PROMPT 调用 LLM 进行更新
     */
    void updateGlobalSummary(String sessionId, String userMessage, String aiResponse);

    /**
     * 更新全局摘要，同时返回 token 用量统计
     * @return token 用量统计
     */
    TokenUsageVO updateGlobalSummaryWithUsage(String sessionId, String userMessage, String aiResponse);

    /**
     * 获取当前的全局摘要
     * @return 当前的全局摘要文本
     */
    String getCurrentSummary(String sessionId);


}