package com.memosystem.service;

import com.memosystem.common.model.ParsedMessage;

/**
 * 消息处理服务接口
 * 负责解析和处理用户输入的消息
 */
public interface MessageProcessService {
    /**
     * 解析用户消息
     * @param userMessage 用户输入的原始消息
     * @return 解析后的消息对象
     */
    ParsedMessage parseMessage(String userMessage);

    /**
     * 检查消息是否为空
     * @param message 要检查的消息
     * @return true 如果消息为空，false 否则
     */
    boolean isEmptyMessage(String message);

    /**
     * 清理和规范化消息
     * @param message 原始消息
     * @return 清理后的消息
     */
    String cleanMessage(String message);
}