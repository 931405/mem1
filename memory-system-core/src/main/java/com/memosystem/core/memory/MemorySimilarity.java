package com.memosystem.core.memory;

import com.memosystem.core.conversation.MessagePair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆相似性数据模型
 * 用于表示检索到的相似历史记忆及其相似度分数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemorySimilarity {
    private String memoryId;                  // 向量数据库中的记忆ID
    private CandidateMemory candidateMemory;  // 记忆事实
    private MessagePair messagePair;          // 对应的对话消息对
    private double similarityScore;           // 相似度分数(0-1)
}
