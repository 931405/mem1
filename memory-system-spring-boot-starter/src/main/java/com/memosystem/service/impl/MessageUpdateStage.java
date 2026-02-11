package com.memosystem.service.impl;

import com.memosystem.adapter.llm.LLMClient;
import com.memosystem.vo.LLMResponseVO;
import com.memosystem.vo.TokenUsageVO;
import com.memosystem.adapter.storage.QdrantLocalClient;
import com.memosystem.config.MemorySystemProperties;
import com.memosystem.config.MemoryPrompts;
import com.memosystem.core.conversation.MessagePair;
import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.core.memory.MemorySimilarity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("mem0MessageUpdateStage")
@Slf4j
public class MessageUpdateStage {
    @Autowired
    private com.memosystem.service.EmbeddingService embeddingService;
    @Autowired
    private QdrantLocalClient vectorDb;

    @Autowired
    @Qualifier("mem0DecisionLLMClient")
    private LLMClient llmClient;

    @Autowired
    private MemorySystemProperties memoryConfig;

    /**
     * 根据候选记忆做出决策并执行相应的记忆操作
     * 对于每个候选记忆：
     * 1. 获取其向量表示
     * 2. 在向量数据库中搜索语义最相似的已有记忆
     * 3. 将候选记忆与相似记忆一同提交给LLM进行决策
     * 4. LLM决策执行四种操作之一：ADD、UPDATE、DELETE、NOOP
     */
    public void makeDecisionAndAction(String sessionId, List<CandidateMemory> candidateMemories) {
        if (candidateMemories == null || candidateMemories.isEmpty()) {
            log.info("没有候选记忆需要处理");
            return;
        }

        log.info("开始处理候选记忆，数量: {}", candidateMemories.size());

        for (int idx = 0; idx < candidateMemories.size(); idx++) {
            CandidateMemory candidateMemory = candidateMemories.get(idx);
            try {
                log.debug("处理候选记忆 [{}/{}]: {}", idx + 1, candidateMemories.size(), candidateMemory.getFact());
                processCandidate(sessionId, candidateMemory);
            } catch (Exception e) {
                log.error("处理候选记忆失败: {}", candidateMemory.getFact(), e);
            }
        }

        log.info("所有候选记忆处理完成");
    }

    /**
     * 处理单个候选记忆，用于并行处理
     */
    public void processSingleCandidate(String sessionId, CandidateMemory candidateMemory) {
        processSingleCandidateWithUsage(sessionId, candidateMemory);
    }

    /**
     * 处理单个候选记忆，返回 token 用量统计
     */
    public TokenUsageVO processSingleCandidateWithUsage(String sessionId, CandidateMemory candidateMemory) {
        if (candidateMemory == null) {
            log.warn("候选记忆为空");
            return new TokenUsageVO(0, 0, 0);
        }
        try {
            TokenUsageVO tokenUsage = processCandidate(sessionId, candidateMemory);
            log.debug("候选记忆处理成功: {}", candidateMemory.getFact());
            return tokenUsage;
        } catch (Exception e) {
            log.error("处理候选记忆异常: {}", candidateMemory.getFact(), e);
            throw new RuntimeException("处理候选记忆失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理单个候选记忆
     * 流程步骤:
     * [步骤4] 获取向量并执行搜索 → 候选旧记忆
     * [步骤5] 构建决策提示
     * [步骤6] LLM决策
     * [步骤7] 执行操作
     * @return token 用量统计，如果没有LLM调用则返回 0
     */
    private TokenUsageVO processCandidate(String sessionId, CandidateMemory candidateMemory) {
        log.debug("步骤1: 生成候选记忆的向量表示 - 事实: {}", candidateMemory.getFact());

        // 步骤1：获取候选记忆的向量表示
        String candidateText = candidateMemory.getFact();
        List<Float> candidateEmbedding = embeddingService.embed(candidateText);
        log.debug("向量生成完成，维度: {}", candidateEmbedding.size());

        // 步骤2：在向量数据库中检索相似的已有记忆
        log.debug("步骤2: 搜索相似的已有记忆");
        List<MemorySimilarity> similarMemories = vectorDb.searchSimilarMemoriesWithScore(sessionId, candidateEmbedding,
                memoryConfig.getUpdateSearchTopK());
        log.debug("相似记忆搜索完成，找到: {} 条", similarMemories.size());

        // 步骤3：构建决策提示
        log.debug("步骤3: 构建LLM决策提示");
        String decisionPrompt = buildDecisionPrompt(candidateMemory, similarMemories);

        // 步骤4：调用LLM进行决策（带容错处理）
        log.debug("步骤4: 调用LLM进行决策");
        String action;
        TokenUsageVO tokenUsage = null;
        try {
            LLMResponseVO llmResponse = llmClient.chatWithUsage(List.of(decisionPrompt));
            tokenUsage = llmResponse.getTokenUsage();
            log.info("LLM决策 token 用量: prompt={}, completion={}, total={}",
                    tokenUsage.getPromptTokens(),
                    tokenUsage.getCompletionTokens(),
                    tokenUsage.getTotalTokens());
            action = parseLLMDecision(llmResponse.getContent());
            log.debug("LLM决策完成，结果: {}", action);
        } catch (Exception e) {
            log.warn("LLM决策调用失败，使用备选策略: {}", e.getMessage());
            action = makeDecisionByFallback(candidateMemory, similarMemories);
            tokenUsage = new TokenUsageVO(0, 0, 0);
            log.debug("备选策略决策完成，结果: {}", action);
        }

        // 步骤5：执行操作
        log.debug("步骤5: 执行数据库操作 - {}", action);
        executeAction(sessionId, action, candidateMemory, candidateEmbedding, similarMemories);
        
        return tokenUsage;
    }

    /**
     * 构建提交给LLM的决策提示
     */
    private String buildDecisionPrompt(
            CandidateMemory candidateMemory,
            List<MemorySimilarity> similarMemories) {

        StringBuilder prompt = new StringBuilder();
        // 使用提示词模板作为基础
        prompt.append(MemoryPrompts.CHINESE_MEMORY_DECISION_PROMPT).append("\n\n");

        prompt.append("【新提取的事实】\n");
        prompt.append("事实：").append(candidateMemory.getFact()).append("\n");
        prompt.append("分类：").append(candidateMemory.getCategory()).append("\n");
        prompt.append("置信度：").append(String.format("%.2f", candidateMemory.getConfidence())).append("\n\n");

        if (!similarMemories.isEmpty()) {
            prompt.append("【现有记忆】\n");
            for (int i = 0; i < similarMemories.size(); i++) {
                MemorySimilarity sim = similarMemories.get(i);
                prompt.append(String.format("相似记忆%d（语义相似度：%.2f%%）:\n", i + 1, sim.getSimilarityScore() * 100));
                prompt.append("  事实：").append(sim.getCandidateMemory().getFact()).append("\n");
                prompt.append("  分类：").append(sim.getCandidateMemory().getCategory()).append("\n");
                prompt.append("  用户原始消息：").append(sim.getMessagePair().getUserMessage()).append("\n");
                // prompt.append("
                // AI响应：").append(sim.getMessagePair().getAiResponse()).append("\n\n");
            }
        } else {
            prompt.append("【现有相似记忆】：无\n\n");
        }

        return prompt.toString();
    }

    /**
     * 解析LLM的决策结果
     * 支持两种格式：
     * 1. 单个操作：{"action": "ADD"} 或 直接文本 "ADD"
     * 2. 列表格式：{"memory": [{"id": "0", "event": "ADD"}, ...]}
     */
    private String parseLLMDecision(String llmResponse) {
        try {
            // 首先尝试解析 JSON 列表格式
            if (llmResponse.contains("\"memory\"") && llmResponse.contains("[")) {
                try {
                    // 提取 JSON 部分（去掉后面的说明文字）
                    String jsonPart = extractJsonPart(llmResponse);

                    com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(jsonPart)
                            .getAsJsonObject();
                    com.google.gson.JsonArray memoryArray = jsonObject.getAsJsonArray("memory");

                    if (memoryArray != null && memoryArray.size() > 0) {
                        // 提取第一个记忆的操作（id "0" 对应当前候选记忆）
                        com.google.gson.JsonObject firstMemory = memoryArray.get(0).getAsJsonObject();
                        String event = firstMemory.has("event") ? firstMemory.get("event").getAsString() : "";

                        // 规范化操作名称
                        event = event.toUpperCase();
                        if (event.contains("ADD")) {
                            log.debug("解析结果: ADD 操作");
                            return "ADD";
                        }
                        if (event.contains("UPDATE")) {
                            log.debug("解析结果: UPDATE 操作");
                            return "UPDATE";
                        }
                        if (event.contains("DELETE")) {
                            log.debug("解析结果: DELETE 操作");
                            return "DELETE";
                        }
                        if (event.contains("NOOP") || event.contains("NONE")) {
                            log.debug("解析结果: NOOP 操作");
                            return "NOOP";
                        }
                    }
                } catch (Exception e) {
                    log.debug("JSON 列表格式解析失败，尝试其他方式: {}", e.getMessage());
                }
            }

            // 如果不是列表格式或解析失败，尝试传统方式解析
            String upperResponse = llmResponse.toUpperCase();

            if (upperResponse.contains("\"ACTION\"") || upperResponse.contains("ACTION")) {
                // 如果包含 "ACTION" 字段，尝试提取值
                if (upperResponse.contains("ADD")) {
                    log.debug("解析结果: ADD 操作");
                    return "ADD";
                }
                if (upperResponse.contains("UPDATE")) {
                    log.debug("解析结果: UPDATE 操作");
                    return "UPDATE";
                }
                if (upperResponse.contains("DELETE")) {
                    log.debug("解析结果: DELETE 操作");
                    return "DELETE";
                }
                if (upperResponse.contains("NOOP")) {
                    log.debug("解析结果: NOOP 操作");
                    return "NOOP";
                }
            }

            // 直接匹配关键词
            if (upperResponse.contains("ADD")) {
                log.debug("解析结果: ADD 操作");
                return "ADD";
            }
            if (upperResponse.contains("UPDATE")) {
                log.debug("解析结果: UPDATE 操作");
                return "UPDATE";
            }
            if (upperResponse.contains("DELETE")) {
                log.debug("解析结果: DELETE 操作");
                return "DELETE";
            }
            if (upperResponse.contains("NOOP")) {
                log.debug("解析结果: NOOP 操作");
                return "NOOP";
            }

            // 默认返回 NOOP
            log.warn("无法识别操作类型，默认返回 NOOP");
            return "NOOP";
        } catch (Exception e) {
            log.error("解析 LLM 决策时出错", e);
            return "NOOP";
        }
    }

    /**
     * 从 LLM 响应中提取纯 JSON 部分
     */
    private String extractJsonPart(String response) {
        // 查找第一个 { 和最后一个 }
        int firstBrace = response.indexOf('{');
        int lastBrace = response.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1);
        }

        return response;
    }

    /**
     * 根据决策执行相应的记忆操作
     */
    private void executeAction(
            String sessionId,
            String action,
            CandidateMemory candidateMemory,
            List<Float> candidateEmbedding,
            List<MemorySimilarity> similarMemories) {

        log.debug("执行操作: {}", action);
        switch (action) {
            case "ADD":
                handleAdd(sessionId, candidateMemory, candidateEmbedding);
                break;
            case "UPDATE":
                handleUpdate(sessionId, candidateMemory, candidateEmbedding, similarMemories);
                break;
            case "DELETE":
                handleDelete(sessionId, similarMemories);
                break;
            case "NOOP":
                handleNoop();
                break;
            default:
                log.warn("未知操作: {}", action);
        }
    }

    /**
     * 处理 ADD 操作：添加新的记忆到向量数据库
     */
    private void handleAdd(String sessionId, CandidateMemory candidateMemory, List<Float> embedding) {
        try {
            // 创建消息对（这里使用候选记忆的事实作为消息内容）
            MessagePair messagePair = new MessagePair(
                    sessionId,
                    "Auto: " + candidateMemory.getFact(),
                    "Added to memory",
                    System.currentTimeMillis());

            // 直接调用 vectorDb 添加到向量数据库
            vectorDb.upsertMemory(sessionId, messagePair, embedding, candidateMemory);
            log.debug("新记忆已添加: {}", candidateMemory.getFact());
        } catch (Exception e) {
            log.error("添加记忆失败", e);
        }
    }

    /**
     * 处理 UPDATE 操作：更新现有记忆的内容
     * 保持原有的消息对（UserMessage 和 AiResponse），只更新 CandidateMemory 部分
     */
    private void handleUpdate(
            String sessionId,
            CandidateMemory candidateMemory,
            List<Float> embedding,
            List<MemorySimilarity> similarMemories) {

        try {
            if (similarMemories.isEmpty()) {
                log.debug("未找到相似记忆，自动转换为ADD操作");
                handleAdd(sessionId, candidateMemory, embedding);
                return;
            }

            // 更新最相似的记忆
            MemorySimilarity mostSimilar = similarMemories.get(0);

            // 生成新的向量表示
            String text = candidateMemory.getFact() + " " + candidateMemory.getCategory();
            List<Float> newEmbedding = embeddingService.embed(text);

            // 直接调用 vectorDb 更新记忆
            vectorDb.updateMemory(sessionId, mostSimilar.getMemoryId(), newEmbedding, candidateMemory);
            log.debug("记忆已更新 - ID: {}, 事实: {}", mostSimilar.getMemoryId(), candidateMemory.getFact());
        } catch (Exception e) {
            log.error("更新记忆失败", e);
        }
    }

    /**
     * 处理 DELETE 操作：删除相矛盾的现有记忆
     */
    private void handleDelete(String sessionId, List<MemorySimilarity> similarMemories) {
        try {
            if (similarMemories.isEmpty()) {
                log.debug("无相似记忆可删除");
                return;
            }

            // 删除最相似的（最可能矛盾的）记忆
            MemorySimilarity mostSimilar = similarMemories.get(0);
            vectorDb.deleteMemory(mostSimilar.getMemoryId());
            log.debug("记忆已删除 - ID: {}, 事实: {}", mostSimilar.getMemoryId(), mostSimilar.getCandidateMemory().getFact());
        } catch (Exception e) {
            log.error("删除记忆失败", e);
        }
    }

    /**
     * 处理 NONE 操作：不执行任何操作（重复或无关的候选记忆）
     */
    private void handleNoop() {
        log.debug("跳过该候选记忆: 无需更新");
    }

    /**
     * 备选决策策略：当 LLM 调用失败时，使用启发式方法进行决策
     * 规则：
     * 1. 如果没有相似的记忆，添加新记忆 (ADD)
     * 2. 如果最高相似度 > 0.8，更新现有记忆 (UPDATE)
     * 3. 如果置信度 > 0.9，添加新记忆 (ADD)
     * 4. 否则，不操作 (NONE)
     */
    private String makeDecisionByFallback(
            CandidateMemory candidateMemory,
            List<MemorySimilarity> similarMemories) {

        log.debug("使用备选决策策略");

        // 规则1：没有相似记忆，直接添加
        if (similarMemories.isEmpty()) {
            return "ADD";
        }

        double maxSimilarity = similarMemories.get(0).getSimilarityScore();

        // 规则2：相似度很高，更新现有记忆
        if (maxSimilarity > 0.85) {
            return "UPDATE";
        }

        // 规则3：候选记忆置信度很高，添加为新记忆
        if (candidateMemory.getConfidence() > 0.9) {
            return "ADD";
        }

        // 规则4：中等相似度（0.7-0.85），考虑添加
        if (maxSimilarity > 0.7 && candidateMemory.getConfidence() > 0.7) {
            return "ADD";
        }

        // 默认：不操作
        return "NONE";
    }

}