package com.memosystem.vo;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提示词响应 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponseVO {
    @Parameter(description = "用户原始消息")
    private String userMessage;

    @Parameter(description = "构建完成的完整提示词")
    private String builtPrompt;

    @Parameter(description = "请求时间戳")
    private long timestamp;
}
