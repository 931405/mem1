package com.memosystem.vo;

import com.memosystem.core.memory.CandidateMemory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 记忆提取结果包装类
 * 同时携带候选记忆列表和 token 用量统计
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemoryExtractionResultVO {
    /** 候选记忆列表 */
    private List<CandidateMemory> candidateMemories;
    /** Token 用量统计 */
    private TokenUsageVO tokenUsage;
}
