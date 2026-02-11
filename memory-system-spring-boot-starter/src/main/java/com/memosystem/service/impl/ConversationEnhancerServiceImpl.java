package com.memosystem.service.impl;

import com.memosystem.adapter.llm.LLMClient;
import com.memosystem.common.model.ParsedMessage;
import com.memosystem.config.MemoryPrompts;
import com.memosystem.service.ConversationEnhancerService;
import com.memosystem.vo.LLMResponseVO;
import com.memosystem.vo.TokenUsageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话增强服务实现
 * 负责构建增强的提示词并获取 AI 响应
 */
@Service("mem0ConversationEnhancerService")
@Slf4j
public class ConversationEnhancerServiceImpl implements ConversationEnhancerService {

    @Autowired
    @Qualifier("mem0DefaultLLMClient")
    private LLMClient llmClient;

    /**
     * 基于用户消息、记忆上下文和全局上下文构建增强的提示词
     * 
     * @param userMessage        用户输入的消息
     * @param memoryContext      相关的历史记忆上下文
     * @param globalContext      全局摘要和上下文
     * @param shortMemoryContext
     * @return
     */
    @Override
    public String buildEnhancedPrompt(String userMessage, String memoryContext, String globalContext,
            String shortMemoryContext) {
        log.debug("构建增强提示词 - 用户消息长度: {}, 记忆上下文长度: {}, 全局上下文长度: {}",
                userMessage.length(),
                memoryContext != null ? memoryContext.length() : 0,
                globalContext != null ? globalContext.length() : 0);

        StringBuilder prompt = new StringBuilder();

        // 使用系统提示词模板
        prompt.append(MemoryPrompts.MEMORY_ANSWER_PROMPT).append("\n");

        // 全局上下文（如果存在）
        if (globalContext != null && !globalContext.isEmpty()) {
            prompt.append("【全局上下文】\n");
            prompt.append(globalContext).append("\n\n");
        }

        // 相关记忆上下文
        if (memoryContext != null && !memoryContext.isEmpty()) {
            prompt.append("【相关记忆】\n");
            prompt.append(memoryContext).append("\n\n");
        } else {
            prompt.append("【相关记忆】\n暂无相关历史记忆。\n\n");
        }

        // 当前用户消息
        prompt.append("【用户问题】\n");
        prompt.append(userMessage).append("\n");
        prompt.append("请基于以上内容，提供详细且有帮助的回答。\n");
        return prompt.toString();
    }

    /**
     * 构建系统指令，包含记忆上下文和全局上下文、简短记忆上下文
     * 
     * @param parsedMessage
     * @param memoryContext
     * @param globalContext
     * @param shortMemoryContext
     * @return
     */
    @Override
    public String buildSystemInstruction(ParsedMessage parsedMessage, String memoryContext, String globalContext,
            String shortMemoryContext) {
        return buildEnhancedPrompt(
                parsedMessage.getContent(),
                memoryContext,
                globalContext,
                shortMemoryContext);
    }

    @Override
    public String buildSystemInstruction(String memoryContext, String globalContext, String shortMemoryContext) {
        StringBuilder prompt = new StringBuilder();

        // 全局上下文（如果存在）
        if (globalContext != null && !globalContext.isEmpty()) {
            prompt.append("【全局上下文】\n");
            prompt.append(globalContext).append("\n\n");
        }

        // 短期记忆上下文
        if (shortMemoryContext != null && !shortMemoryContext.isEmpty()) {
            prompt.append("【近期对话历史】\n");
            prompt.append(shortMemoryContext).append("\n\n");
        }

        // 相关记忆上下文
        if (memoryContext != null && !memoryContext.isEmpty()) {
            prompt.append("【相关记忆】\n");
            prompt.append(memoryContext).append("\n\n");
        } else {
            prompt.append("【相关记忆】\n暂无相关历史记忆。\n\n");
        }

        return prompt.toString();
    }

    @Override
    public String getAIResponse(String enhancedPrompt) {
        log.debug("调用 LLM 获取响应 - 使用默认模型");
        // 直接调用，模型已在注入的 LLMClient 中配置
        return getAIResponse(enhancedPrompt, null);
    }

    @Override
    public LLMResponseVO getAIResponseWithUsage(String enhancedPrompt) {
        log.info("调用 LLM 获取响应（含 token 统计） - 提示词长度: {}", enhancedPrompt.length());

        try {
            List<String> messages = new ArrayList<>();
            messages.add(enhancedPrompt);
            LLMResponseVO response = llmClient.chatWithUsage(messages);
            log.info("LLM 响应已获取，长度: {}，token 用量: prompt={}, completion={}, total={}",
                    response.getContent().length(),
                    response.getTokenUsage().getPromptTokens(),
                    response.getTokenUsage().getCompletionTokens(),
                    response.getTokenUsage().getTotalTokens());
            return response;

        } catch (Exception e) {
            log.error("调用 LLM 时出错", e);
            // 如果 LLM 调用失败，返回友好的错误提示，token 用量为 0
            return new LLMResponseVO(
                    "抱歉，我现在无法生成响应。错误信息: " + e.getMessage(),
                    new TokenUsageVO(0, 0, 0));
        }
    }

    @Override
    public String getAIResponse(String enhancedPrompt, String model) {
        log.info("调用 LLM 获取响应 - 模型: {}, 提示词长度: {}", model, enhancedPrompt.length());

        try {
            // 调用 LLMClient 获取响应（带 token 统计）
            List<String> messages = new ArrayList<>();
            messages.add(enhancedPrompt);
            LLMResponseVO llmResponse = llmClient.chatWithUsage(messages);
            log.info("LLM 响应已获取，长度: {}，token 用量: prompt={}, completion={}, total={}",
                    llmResponse.getContent().length(),
                    llmResponse.getTokenUsage().getPromptTokens(),
                    llmResponse.getTokenUsage().getCompletionTokens(),
                    llmResponse.getTokenUsage().getTotalTokens());
            return llmResponse.getContent();

        } catch (Exception e) {
            log.error("调用 LLM 时出错", e);
            // 如果 LLM 调用失败，返回友好的错误提示
            return "抱歉，我现在无法生成响应。错误信息: " + e.getMessage();
        }
    }
}