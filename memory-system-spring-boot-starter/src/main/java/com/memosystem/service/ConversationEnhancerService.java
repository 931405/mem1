package com.memosystem.service;

import com.memosystem.common.model.ParsedMessage;
import com.memosystem.vo.LLMResponseVO;

/**
 * 对话增强服务接口
 * 负责构建增强的提示词并获取 AI 响应
 */
public interface ConversationEnhancerService {

    /**
     * 基于用户消息、记忆上下文和全局上下文构建增强的提示词
     * 
     * @param userMessage   用户输入的消息
     * @param memoryContext 相关的历史记忆上下文
     * @param globalContext 全局摘要和上下文
     * @return 增强后的提示词
     */
    String buildEnhancedPrompt(String userMessage, String memoryContext, String globalContext,
            String shortMemoryContext);

    /**
     * 构建系统指令，包含记忆上下文和全局上下文、简短记忆上下文
     * 
     * @param parsedMessage
     * @param memoryContext
     * @param globalContext
     * @param shortMemoryContext
     * @return
     */
    String buildSystemInstruction(ParsedMessage parsedMessage, String memoryContext, String globalContext,
            String shortMemoryContext);

    /**
     * 构建系统指令，包含记忆上下文和全局上下文、简短记忆上下文
     * 
     * @param memoryContext
     * @param globalContext
     * @param shortMemoryContext
     * @return
     */
    String buildSystemInstruction(String memoryContext, String globalContext, String shortMemoryContext);

    /**
     * 调用 LLM 获取对提示词的响应
     * 
     * @param enhancedPrompt 增强后的提示词
     * @return LLM 生成的响应文本
     */
    String getAIResponse(String enhancedPrompt);

    /**
     * 调用 LLM 获取对提示词的响应，同时返回 token 用量统计
     * 
     * @param enhancedPrompt 增强后的提示词
     * @return 包含响应内容和 token 用量的 LLMResponseVO
     */
    LLMResponseVO getAIResponseWithUsage(String enhancedPrompt);

    /**
     * 调用 LLM 获取对提示词的响应，使用指定模型
     * 
     * @param enhancedPrompt 增强后的提示词
     * @param model          指定的 LLM 模型
     * @return LLM 生成的响应文本
     */
    String getAIResponse(String enhancedPrompt, String model);
}