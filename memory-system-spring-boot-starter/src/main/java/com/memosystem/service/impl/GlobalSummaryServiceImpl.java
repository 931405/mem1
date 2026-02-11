package com.memosystem.service.impl;

import com.memosystem.adapter.llm.LLMClient;
import com.memosystem.common.model.CommonFileRepository;
import com.memosystem.config.MemoryPrompts;
import com.memosystem.core.summary.GlobalSummaryEntry;
import com.memosystem.service.GlobalSummaryService;
import com.memosystem.vo.LLMResponseVO;
import com.memosystem.vo.TokenUsageVO;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 全局摘要服务实现
 * 负责生成和维护用户的全局记忆摘要
 */
@Service("mem0GlobalSummaryService")
@Slf4j
public class GlobalSummaryServiceImpl implements GlobalSummaryService {

    private String currentSummary;
    private int messageCount = 0;

    @Autowired
    @Qualifier("mem0GlobalMemoryLLMClient")
    private LLMClient llmClient;

    @Override
    public void updateGlobalSummary(String sessionId, String userMessage, String aiResponse) {
        updateGlobalSummaryWithUsage(sessionId, userMessage, aiResponse);
    }

    @Override
    public TokenUsageVO updateGlobalSummaryWithUsage(String sessionId, String userMessage, String aiResponse) {

        if (userMessage.isEmpty() && aiResponse.isEmpty()) {
            log.debug("没有新的消息对，跳过摘要更新");
            return new TokenUsageVO(0, 0, 0);
        }
        String currentSummary = getCurrentSummary(sessionId);
        try {
            // 构建更新提示词
            String updatePrompt = buildGlobalSummaryUpdatePrompt(
                    currentSummary,
                    userMessage,
                    aiResponse,
                    messageCount);

            log.debug("调用 LLM 更新全局摘要");

            // 更新摘要
            List<String> messages = new ArrayList<>();
            messages.add(updatePrompt);
            LLMResponseVO response = llmClient.chatWithUsage(messages);
            log.info("全局摘要更新 token 用量: prompt={}, completion={}, total={}",
                    response.getTokenUsage().getPromptTokens(),
                    response.getTokenUsage().getCompletionTokens(),
                    response.getTokenUsage().getTotalTokens());
            this.currentSummary = response.getContent();
            this.messageCount++;

            // 报错更新后的摘要到持久化存储
            GlobalSummaryEntry summaryEntry = new GlobalSummaryEntry(sessionId, this.currentSummary);
            CommonFileRepository.save("global_summary.json", summaryEntry);
            log.debug("全局摘要更新完成，交互次数：{}", messageCount);
            return response.getTokenUsage();

        } catch (Exception e) {
            log.warn("使用备选策略更新全局摘要：{}", e.getMessage());
            // 备选策略：简单追加新的对话
            appendToSummary(sessionId, userMessage, aiResponse);
            return new TokenUsageVO(0, 0, 0);
        }
    }

    /**
     * 备选策略：直接追加新的对话到摘要中
     */
    private void appendToSummary(String sessionId, String userMessage, String aiResponse) {
        StringBuilder summary = new StringBuilder(this.currentSummary != null ? this.currentSummary : "");

        if (!summary.toString().contains("【交互历史】")) {
            summary.append("\n【交互历史】\n");
        }

        summary.append("\n--- 消息 ").append(messageCount + 1).append(" ---\n");
        summary.append("用户: ").append(userMessage.substring(0, Math.min(100, userMessage.length()))).append("\n");
        summary.append("AI: ").append(aiResponse.substring(0, Math.min(100, aiResponse.length()))).append("\n");

        this.currentSummary = summary.toString();
        this.messageCount++;
        // 报错更新后的摘要到持久化存储
        GlobalSummaryEntry summaryEntry = new GlobalSummaryEntry(sessionId, this.currentSummary);
        CommonFileRepository.save("global_summary.json", summaryEntry);
    }

    /**
     * 构建全局摘要更新提示词
     */
    private String buildGlobalSummaryUpdatePrompt(
            String previousSummary,
            String userMessage,
            String aiResponse,
            int messageCount) {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        return String.format("""
                %s

                ═════════════════════════════════════════════════════════════════════════════════
                📋 当前全局摘要（第 %d 个交互）：
                ═════════════════════════════════════════════════════════════════════════════════

                %s

                ═════════════════════════════════════════════════════════════════════════════════
                📋 新增对话（时间戳：%s）：
                ═════════════════════════════════════════════════════════════════════════════════

                【用户消息】
                %s

                【AI 响应】
                %s

                ═════════════════════════════════════════════════════════════════════════════════
                📝 任务要求：
                ═════════════════════════════════════════════════════════════════════════════════

                请根据上述过程性记忆系统提示词，基于以下信息生成更新后的全局摘要：

                1. 保留当前全局摘要中的所有重要信息
                2. 融合新增对话中的关键信息
                3. 按照过程性记忆的结构来组织摘要
                4. 确保摘要逻辑清晰、层次分明
                5. 摘要长度保持在 500-1000 字符范围内
                6. 保持中文表述风格，简洁准确

                输出仅包含更新后的全局摘要内容，不需要额外说明。
                """,
                MemoryPrompts.PROCEDURAL_MEMORY_SYSTEM_PROMPT,
                messageCount + 1,
                previousSummary,
                timestamp,
                userMessage,
                aiResponse);
    }

    @Override
    public String getCurrentSummary(String sessionId) {
        Optional<GlobalSummaryEntry> currentSummary = CommonFileRepository.loadLastContent(sessionId,
                "global_summary.json", GlobalSummaryEntry.class);
        return currentSummary.map(GlobalSummaryEntry::getGlobalSummary).orElse("暂无全局摘要内容。");
    }
}