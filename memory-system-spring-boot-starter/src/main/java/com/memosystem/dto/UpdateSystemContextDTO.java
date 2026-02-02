package com.memosystem.dto;

import lombok.Data;
import lombok.NonNull;

/**
 * 更新系统上下文请求 DTO
 */
@Data
public class UpdateSystemContextDTO {

    @NonNull
    private String sessionId; // 会话ID
    @NonNull
    private String userMessage; // 用户消息
    @NonNull
    private String aiResponse; // AI回复内容
}
