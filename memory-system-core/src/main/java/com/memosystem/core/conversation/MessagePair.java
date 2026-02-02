package com.memosystem.core.conversation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息对数据模型
 * 用于存储用户消息和对应的AI响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessagePair {
    private String sessionId;    // 会话ID
    private String userMessage;  // 用户输入的消息
    private String aiResponse;   // AI 生成的响应
    private long timestamp;      // 消息创建的时间戳
}
