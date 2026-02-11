package com.memosystem.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token 用量汇总统计
 * 记录整个对话流程中所有 LLM 调用的 token 消耗汇总
 */
@Data
public class TokenUsageSummaryVO {
    /** 总提示词 token 数 */
    private AtomicInteger totalPromptTokens = new AtomicInteger(0);
    /** 总补全 token 数 */
    private AtomicInteger totalCompletionTokens = new AtomicInteger(0);
    /** 总 token 数 */
    private AtomicInteger totalTokens = new AtomicInteger(0);
    /** LLM 调用次数 */
    private AtomicInteger callCount = new AtomicInteger(0);
    /** 各阶段的详细 token 用量 */
    private List<TokenUsageDetailVO> details = new ArrayList<>();

    /**
     * 添加单次 LLM 调用的 token 统计
     */
    public synchronized void addUsage(String stage, TokenUsageVO usage) {
        if (usage != null) {
            totalPromptTokens.addAndGet(usage.getPromptTokens());
            totalCompletionTokens.addAndGet(usage.getCompletionTokens());
            totalTokens.addAndGet(usage.getTotalTokens());
            callCount.incrementAndGet();
            details.add(new TokenUsageDetailVO(stage, usage));
        }
    }

    /**
     * 获取总提示词 token 数
     */
    public int getTotalPromptTokens() {
        return totalPromptTokens.get();
    }

    /**
     * 获取总补全 token 数
     */
    public int getTotalCompletionTokens() {
        return totalCompletionTokens.get();
    }

    /**
     * 获取总 token 数
     */
    public int getTotalTokens() {
        return totalTokens.get();
    }

    /**
     * 获取调用次数
     */
    public int getCallCount() {
        return callCount.get();
    }
}
