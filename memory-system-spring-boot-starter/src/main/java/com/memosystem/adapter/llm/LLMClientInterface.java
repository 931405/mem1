package com.memosystem.adapter.llm;

import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.vo.LLMResponseVO;
import com.memosystem.vo.MemoryExtractionResultVO;

import java.util.List;

/**
 * LLM 客户端接口
 * 定义与大语言模型交互的标准接口
 * 
 * 使用方可以实现此接口来支持不同的 LLM 提供商
 */
public interface LLMClientInterface {

    /**
     * 调用 LLM 进行对话
     * 
     * @param messages 消息列表（交替的 user/assistant 消息）
     * @return LLM 的回复内容
     * @throws RuntimeException 如果调用失败
     */
    String chat(List<String> messages);

    /**
     * 调用 LLM 进行对话，同时返回 token 用量统计
     * 
     * @param messages 消息列表（交替的 user/assistant 消息）
     * @return 包含回复内容和 token 用量的 LLMResponseVO
     * @throws RuntimeException 如果调用失败
     */
    LLMResponseVO chatWithUsage(List<String> messages);

    /**
     * 根据用户消息生成 AI 响应
     * 
     * @param userMessage 用户消息
     * @return AI 响应
     * @throws RuntimeException 如果调用失败
     */
    String generateResponse(String userMessage);

    /**
     * 从提示词中提取候选记忆
     * 
     * @param prompt 提示词
     * @return 候选记忆列表
     * @throws RuntimeException 如果提取失败
     */
    List<CandidateMemory> formCandidateMemories(String prompt);

    /**
     * 从提示词中提取候选记忆，同时返回 token 用量统计
     * 
     * @param prompt 提示词
     * @return 包含候选记忆列表和 token 用量的 MemoryExtractionResultVO
     * @throws RuntimeException 如果提取失败
     */
    MemoryExtractionResultVO formCandidateMemoriesWithUsage(String prompt);
}
