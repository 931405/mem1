package com.memosystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话请求 DTO
 * 支持调用者自定义模型和参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {

    /**
     * 会话ID（必填）
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 用户消息（必填）
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 指定使用的模型（可选）
     * 如果不指定，使用系统默认模型
     */
    private String model;

    /**
     * 温度参数（可选，0.0-1.0）
     * 控制回复的随机性
     */
    private Double temperature;

    /**
     * 最大token数（可选）
     */
    private Integer maxTokens;
}
