package com.memosystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 用量统计
 * 记录单次 LLM 调用的 token 消耗
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenUsageVO {
    /** 提示词 token 数 */
    private int promptTokens;
    /** 补全 token 数 */
    private int completionTokens;
    /** 总 token 数 */
    private int totalTokens;
}
