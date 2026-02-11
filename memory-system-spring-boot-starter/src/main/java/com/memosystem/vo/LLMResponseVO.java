package com.memosystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 响应包装类
 * 同时携带回复内容和 token 用量统计
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LLMResponseVO {
    /** AI 回复内容 */
    private String content;
    /** Token 用量统计 */
    private TokenUsageVO tokenUsage;
}
