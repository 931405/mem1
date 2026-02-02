package com.memosystem.service;

import com.memosystem.common.model.Result;
import lombok.NonNull;
import java.util.Map;

public interface ConversationService {
    // 入参: 用户消息
    // 出参: 包含AI回复和相关上下文的完整结果
    Map<String, Object> processConversation(String sessionId, String userMessage);

    /**
     * 获取构建完成的提示词（包含相似记忆的完整上下文）
     * 
     * @param userMessage 用户消息
     * @return 包含相似记忆的完整提示词
     */
    String buildPromptWithMemories(String sessionId, String userMessage);

    /**
     * 获取构建完成的提示词（仅包含用户可见的记忆上下文）
     * 
     * @param userMessage 用户消息
     * @return 仅包含用户可见记忆的提示词
     */
    String getPrompt(String sessionId, String userMessage);

    /**
     * 更新系统上下文信息
     * 
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @param aiResponse  AI回复内容
     */
    Result<String> updateSystemContext(String sessionId, String userMessage, String aiResponse);
}