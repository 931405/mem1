package com.memosystem.core.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * 候选记忆数据模型
 * 表示系统抽取的新事实或需要更新的记忆
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CandidateMemory {
    private String fact;        // 记忆事实内容
    private String category;    // 记忆分类（如：个人、工作、习惯等）
    private double confidence;  // 置信度(0-1)

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%.2f %s: %s", confidence, category, fact);
    }
}
