package com.memosystem.service.impl;

import com.memosystem.common.model.CommonFileRepository;
import com.memosystem.common.model.ParsedMessage;
import com.memosystem.common.model.Result;
import com.memosystem.config.MemorySystemProperties;
import com.memosystem.core.conversation.MessagePair;
import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.core.memory.MemorySimilarity;
import com.memosystem.core.summary.GlobalSummaryEntry;
import com.memosystem.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 对话服务实现
 * 负责处理完整的对话流程，包括消息处理、记忆检索、AI 响应、记忆抽取和更新
 */
@Service("mem0ConversationService")
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private MessageProcessService messageProcessService;

    @Autowired
    private MemoryRetrieverService memoryRetrieverService;

    @Autowired
    private ConversationEnhancerService conversationEnhancerService;

    @Autowired
    private FactExtractorService factExtractorService;

    @Autowired
    private GlobalSummaryService globalSummaryService;

    @Autowired
    private MemoryUpdateService memoryUpdateService;

    @Autowired
    private MemorySystemProperties memoryConfig;

    @Autowired
    private ShortTermMemoryService shortTermMemoryService;

    @Autowired
    @Qualifier("mem0ThreadPoolExecutor")
    private ExecutorService executorService;

    private static final String MESSAGES_FILE = "messages.json";
    private static final String SUMMARY_FILE = "global_summary.json";

    /**
     * 处理对话
     * 组合调用：构建提示词 → 获取AI响应 → 更新系统上下文
     * 
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @return 对话结果
     */
    @Override
    public Map<String, Object> processConversation(String sessionId, String userMessage) {
        log.info("========== 开始处理对话 ==========");
        log.info("用户消息: {}", userMessage);

        try {
            Map<String, Long> timings = new java.util.LinkedHashMap<>();
            long totalStartTime = System.currentTimeMillis();

            // 阶段 1: 构建增强提示词（包含三层记忆）
            log.info("【阶段 1】构建增强提示词...");
            long phase1Start = System.currentTimeMillis();
            String enhancedPrompt = buildPromptWithMemories(sessionId, userMessage);
            long phase1Duration = System.currentTimeMillis() - phase1Start;
            timings.put("阶段1-构建提示词", phase1Duration);
            log.info("提示词构建完成，长度: {} 字符，耗时: {} ms", enhancedPrompt.length(), phase1Duration);

            // 阶段 2: 获取 AI 响应
            log.info("【阶段 2】调用 LLM 获取 AI 响应...");
            long phase2Start = System.currentTimeMillis();
            String aiResponse = conversationEnhancerService.getAIResponse(enhancedPrompt);
            long phase2Duration = System.currentTimeMillis() - phase2Start;
            timings.put("阶段2-LLM响应", phase2Duration);
            log.info("AI 响应已获取，长度: {} 字符，耗时: {} ms", aiResponse.length(), phase2Duration);

            // 阶段 3: 更新系统上下文（记忆抽取 + 全局摘要更新）
            log.info("【阶段 3】更新系统上下文...");
            long phase3Start = System.currentTimeMillis();
            updateSystemContext(sessionId, userMessage, aiResponse);
            long phase3Duration = System.currentTimeMillis() - phase3Start;
            timings.put("阶段3-更新上下文", phase3Duration);
            log.info("系统上下文已更新，耗时: {} ms", phase3Duration);

            // 构建并返回结果
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("userMessage", userMessage);
            result.put("aiResponse", aiResponse);
            result.put("timestamp", System.currentTimeMillis());
            result.put("timings", timings);
            result.put("totalDuration", totalDuration);

            log.info("========== 对话处理完成 ==========");
            timings.forEach((step, duration) -> log.info("  {}: {} ms", step, duration));
            log.info("  总耗时: {} ms", totalDuration);

            return result;

        } catch (Exception e) {
            log.error("对话处理异常", e);
            throw new RuntimeException("对话处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取构建用户对话功能的完整的提示词（包含相似记忆的完整上下文）
     * 
     * @param sessionId
     * @param userMessage 用户消息
     * @return
     */
    @Override
    public String buildPromptWithMemories(String sessionId, String userMessage) {
        log.info("构建带相似记忆的提示词，用户消息: {}", userMessage);

        try {
            // 步骤 1: 解析消息
            ParsedMessage parsedMessage = messageProcessService.parseMessage(userMessage);

            // 步骤 2: 检索相似的历史记忆（长期语义记忆）
            List<MemorySimilarity> relatedMemories = memoryRetrieverService.retrieveSimilarMemories(sessionId,
                    parsedMessage, memoryConfig.getConversationSearchTopK());

            // 步骤 3: 获取短期记忆上下文
            String shortTermContext = shortTermMemoryService.buildShortTermContext(sessionId);
            log.debug("短期记忆: {} 轮对话", shortTermMemoryService.getMessageCount(sessionId));

            // 步骤 4: 获取全局上下文并构建完整的提示词
            String globalContext = globalSummaryService.getCurrentSummary(sessionId);
            String memoryContext = buildMemoryContext(relatedMemories);

            String enhancedPrompt = conversationEnhancerService.buildSystemInstruction(parsedMessage,
                    memoryContext,
                    globalContext,
                    shortTermContext);

            log.debug("提示词构建完成");
            return enhancedPrompt;

        } catch (Exception e) {
            log.error("构建提示词异常", e);
            throw new RuntimeException("构建提示词失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPrompt(String sessionId, String userMessage) {
        log.debug("构建带相似记忆的提示词，用户消息: {}", userMessage);

        try {
            // 步骤 1: 解析消息
            ParsedMessage parsedMessage = messageProcessService.parseMessage(userMessage);

            // 步骤 2: 检索相似的历史记忆（长期语义记忆）
            List<MemorySimilarity> relatedMemories = memoryRetrieverService.retrieveSimilarMemories(sessionId,
                    parsedMessage, memoryConfig.getConversationSearchTopK());
            log.debug("检索到 {} 条相似记忆", relatedMemories.size());

            // 步骤 3: 获取短期记忆上下文
            String shortTermContext = shortTermMemoryService.buildShortTermContext(sessionId);
            log.debug("短期记忆: {} 轮对话", shortTermMemoryService.getMessageCount(sessionId));

            // 步骤 4: 获取全局上下文并构建完整的提示词
            String globalContext = globalSummaryService.getCurrentSummary(sessionId);
            String memoryContext = buildMemoryContext(relatedMemories);

            String enhancedPrompt = conversationEnhancerService.buildSystemInstruction(
                    memoryContext,
                    globalContext,
                    shortTermContext);
            log.debug("提示词构建完成");
            return enhancedPrompt;

        } catch (Exception e) {
            log.error("构建提示词异常", e);
            throw new RuntimeException("构建提示词失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新系统上下文信息
     * 
     * @param sessionId   会话ID
     * @param userMessage 用户消息
     * @param aiResponse  AI回复内容
     * @return
     */
    @Override
    public Result<String> updateSystemContext(String sessionId, String userMessage, String aiResponse) {
        try {
            log.debug("【开始更新系统上下文】");
            log.info("更新系统上下文 - sessionId: {}, 用户消息: {}, AI响应: {}",
                    sessionId, userMessage, aiResponse);
            // 使用Map记录各步骤耗时
            java.util.Map<String, Long> timings = new java.util.LinkedHashMap<>();
            long totalStartTime = System.currentTimeMillis();

            // 步骤 5: 保存消息对
            log.debug("【步骤 5】保存消息对到持久化存储...");
            long step5Start = System.currentTimeMillis();
            MessagePair messagePair = new MessagePair(sessionId, userMessage, aiResponse, System.currentTimeMillis());
            CommonFileRepository.save(MESSAGES_FILE, messagePair);
            long step5Duration = System.currentTimeMillis() - step5Start;
            timings.put("步骤5-保存消息对", step5Duration);

            // 步骤 6: 获取全局摘要和最近记忆
            log.debug("【步骤 6】获取全局摘要和最近记忆...");
            long step6Start = System.currentTimeMillis();
            String globalSummary = globalSummaryService.getCurrentSummary(sessionId);
            String recentMemories = memoryRetrieverService.getRecentMemories(sessionId, MESSAGES_FILE);
            long step6Duration = System.currentTimeMillis() - step6Start;
            timings.put("步骤6-获取上下文", step6Duration);

            // 步骤 6.5：创建临时摘要（追加最新消息）
            log.debug("【步骤 6.5】创建临时摘要（追加最新消息）...");
            long step65Start = System.currentTimeMillis();
            createAndSaveTemporarySummary(sessionId, userMessage, aiResponse, globalSummary);
            long step65Duration = System.currentTimeMillis() - step65Start;
            timings.put("步骤6.5-创建临时摘要", step65Duration);

            // 并行执行步骤 7 和步骤 9
            log.debug("【阶段 2】并行处理：抽取候选记忆 && 更新全局摘要...");
            long phase2Start = System.currentTimeMillis();

            java.util.concurrent.CompletableFuture<List<CandidateMemory>> extractTask = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> {
                        log.debug("【步骤 7】从对话中抽取候选记忆...");
                        long step7Start = System.currentTimeMillis();
                        List<CandidateMemory> extracted = factExtractorService.extractCandidateMemories(
                                globalSummary, recentMemories, userMessage, aiResponse);
                        long step7Duration = System.currentTimeMillis() - step7Start;
                        timings.put("步骤7-抽取候选记忆", step7Duration);
                        return extracted;
                    }, executorService);

            java.util.concurrent.CompletableFuture<Void> updateSummaryTask = java.util.concurrent.CompletableFuture
                    .runAsync(() -> {
                        log.debug("【步骤 9】更新全局摘要...");
                        long step9Start = System.currentTimeMillis();
                        globalSummaryService.updateGlobalSummary(sessionId, userMessage, aiResponse);
                        long step9Duration = System.currentTimeMillis() - step9Start;
                        timings.put("步骤9-更新全局摘要", step9Duration);
                    }, executorService);

            List<CandidateMemory> extractedMemories = extractTask.join();
            long phase2Duration = System.currentTimeMillis() - phase2Start;
            timings.put("阶段2-并行处理", phase2Duration);

            // 步骤 8: 更新记忆库
            log.debug("【步骤 8】更新记忆库...");
            long step8Start = System.currentTimeMillis();
            memoryUpdateService.updateMemories(sessionId, extractedMemories);
            long step8Duration = System.currentTimeMillis() - step8Start;
            timings.put("步骤8-更新记忆库", step8Duration);

            updateSummaryTask.join();

            long totalDuration = System.currentTimeMillis() - totalStartTime;

            // 打印汇总
            log.debug("【耗时汇总】");
            timings.forEach((step, duration) -> log.debug("  {}: {} ms", step, duration));
            log.debug("  总耗时: {} ms", totalDuration);

            return Result.success("系统上下文已成功更新");

        } catch (IllegalArgumentException e) {
            log.error("【参数错误】更新系统上下文失败: {}", e.getMessage());
            return Result.badRequest("参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("【系统异常】更新系统上下文失败", e);
            return Result.businessError("更新系统上下文失败: " + e.getMessage());
        }
    }

    /**
     * 创建并保存临时摘要（追加最新消息对）
     * 
     * @param sessionId
     * @param userMessage
     * @param aiResponse
     * @param globalSummary
     */
    private void createAndSaveTemporarySummary(String sessionId, String userMessage, String aiResponse,
            String globalSummary) {
        StringBuilder tempSummary = new StringBuilder(globalSummary);

        // 添加临时标记部分
        if (!tempSummary.toString().contains("【最近交互】")) {
            tempSummary.append("\n\n【最近交互】\n");
        } else {
            tempSummary.append("\n");
        }

        // 快速追加最新的消息对（标注为临时）
        tempSummary.append("--- 临时消息对 ---\n");
        tempSummary.append("用户：").append(
                userMessage.length() > 200 ? userMessage.substring(0, 200) + "..." : userMessage).append("\n");
        tempSummary.append("AI：").append(
                aiResponse.length() > 200 ? aiResponse.substring(0, 200) + "..." : aiResponse).append("\n");
        // 保存临时摘要到持久化存储
        GlobalSummaryEntry tempEntry = new GlobalSummaryEntry(sessionId, tempSummary.toString());
        CommonFileRepository.save(SUMMARY_FILE, tempEntry);
        log.debug("临时全局摘要已保存");

    }

    /**
     * 构建记忆上下文
     */
    private String buildMemoryContext(List<MemorySimilarity> memorySimilarities) {
        if (memorySimilarities.isEmpty()) {
            return "【记忆库】当前无相关历史记忆\n";
        }

        StringBuilder context = new StringBuilder();
        context.append("【相关历史记忆】\n");
        for (int i = 0; i < memorySimilarities.size(); i++) {
            MemorySimilarity ms = memorySimilarities.get(i);
            CandidateMemory memory = ms.getCandidateMemory();
            MessagePair msgPair = ms.getMessagePair();

            context.append(String.format("记忆 %d（相关度: %.2f%%）\n", i + 1, ms.getSimilarityScore() * 100));
            context.append("  事实: ").append(memory.getFact()).append("\n");
            context.append("  分类: ").append(memory.getCategory()).append("\n");
            context.append("  置信度: ").append(String.format("%.2f", memory.getConfidence())).append("\n");
            if (msgPair != null) {
                context.append("  相关问题: ").append(msgPair.getUserMessage()).append("\n");
            }
            context.append("\n");
        }
        return context.toString();
    }

    /**
     * 构建对话结果
     */
    private Map<String, Object> buildConversationResult(
            String userMessage,
            String aiResponse,
            List<CandidateMemory> extractedMemories,
            List<MemorySimilarity> relatedMemories,
            String globalSummary) {

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("userMessage", userMessage);
        result.put("aiResponse", aiResponse);
        result.put("extractedMemories", extractedMemories);
        result.put("relatedMemories", relatedMemories);
        result.put("timestamp", System.currentTimeMillis());
        result.put("globalSummary", globalSummary);
        result.put("memoryAddedCount", countAddedMemories(extractedMemories));
        result.put("memoryUpdatedCount", countUpdatedMemories(extractedMemories));
        result.put("memoryDeletedCount", 0); // 目前暂无删除操作统计

        return result;
    }

    /**
     * 统计新增的记忆数（简单实现，可根据实际需求调整）
     */
    private int countAddedMemories(List<CandidateMemory> extractedMemories) {
        return Math.min(extractedMemories.size(), 3); // 假设大部分是新增的
    }

    /**
     * 统计更新的记忆数（简单实现，可根据实际需求调整）
     */
    private int countUpdatedMemories(List<CandidateMemory> extractedMemories) {
        return extractedMemories.size() > 3 ? extractedMemories.size() - 3 : 0;
    }
}