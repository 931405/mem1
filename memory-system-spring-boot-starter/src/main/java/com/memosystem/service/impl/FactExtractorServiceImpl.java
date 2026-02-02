package com.memosystem.service.impl;

import com.memosystem.adapter.llm.LLMClient;
import com.memosystem.config.MemoryPrompts;
import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.service.FactExtractorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 事实抽取服务实现
 * 从对话中抽取候选的新记忆事实
 */
@Service("mem0FactExtractorService")
@Slf4j
public class FactExtractorServiceImpl implements FactExtractorService {

    @Autowired
    @Qualifier("mem0MemoryExtractionClient")
    private LLMClient memoryLLMClient;

    /**
     * 从对话中抽取候选记忆
     */
    @Override
    public List<CandidateMemory> extractCandidateMemories(
            String globalSummary,
            String recentMemories,
            String userMessage,
            String aiResponse) {

        return extractCandidateMemories(globalSummary, recentMemories, userMessage, aiResponse, null);
    }

    /**
     * 抽取候选记忆，使用指定的 LLM 模型
     */
    @Override
    public List<CandidateMemory> extractCandidateMemories(
            String globalSummary,
            String recentMemories,
            String userMessage,
            String aiResponse,
            String model) {

        log.debug("抽取候选记忆");

        try {
            // 使用 MemoryPrompts 中的标准提示词模板
            String extractionPrompt = MemoryPrompts.buildCompleteExtractionPrompt(
                    globalSummary != null ? globalSummary : "",
                    recentMemories != null ? recentMemories : "",
                    userMessage,
                    aiResponse);

            // 调用 LLM 进行抽取
            List<CandidateMemory> candidates = memoryLLMClient.formCandidateMemories(extractionPrompt);

            log.debug("抽取完成，得到 {} 个候选记忆", candidates.size());
            return candidates;

        } catch (com.memosystem.common.exception.LLMClientException e) {
            log.error("LLM 调用失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        } catch (com.memosystem.common.exception.JsonParseException e) {
            log.error("解析记忆提取结果失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        } catch (RuntimeException e) {
            log.error("抽取候选记忆时出错: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}