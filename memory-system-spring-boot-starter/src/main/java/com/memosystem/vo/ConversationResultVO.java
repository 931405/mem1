package com.memosystem.vo;

import com.memosystem.core.memory.CandidateMemory;
import com.memosystem.core.memory.MemorySimilarity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话结果数据模型
 * 用于返回给客户端的完整对话处理结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationResultVO {
    private String sessionId; // 会话ID
    private String userMessage; // 用户输入消息
    private String aiResponse; // AI 生成的响应
    private List<CandidateMemory> extractedMemories; // 本次对话抽取的候选记忆
    private List<MemorySimilarity> relatedMemories; // 检索到的相关历史记忆
    private long timestamp; // 处理时间戳
    private String globalSummary; // 更新后的全局摘要

    // 记忆库操作统计
    private int memoryAddedCount; // 新增记忆数
    private int memoryUpdatedCount; // 更新的记忆数
    private int memoryDeletedCount; // 删除的记忆数
}
