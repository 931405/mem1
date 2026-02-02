package com.memosystem.core.memory;

import com.memosystem.core.conversation.MessagePair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量数据库中的记忆条目数据模型
 * 用于在 Qdrant 中存储的完整记忆信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemoryEntry {
    private String id;                         // 向量数据库中的唯一标识符
    private String sessionId;                  // 会话ID
    private MessagePair messagePair;           // 对应的对话消息对
    private List<Float> embedding;            // 向量化的嵌入表示
    private CandidateMemory candidateMemory;  // 关联的候选记忆
}
