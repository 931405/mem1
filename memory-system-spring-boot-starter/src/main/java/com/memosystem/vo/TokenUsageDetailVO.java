package com.memosystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 用量详细信息
 * 记录单个阶段的 LLM 调用 token 统计
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenUsageDetailVO {
    /** 阶段名称 */
    private String stage;
    /** Token 用量 */
    private TokenUsageVO usage;
}
