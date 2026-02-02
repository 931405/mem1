package com.memosystem.service;

import com.memosystem.core.conversation.MessagePair;

import java.util.List;

/**
 * 短期记忆服务接口
 * 管理会话级别的临时记忆缓存
 */
public interface ShortTermMemoryService {

    /**
     * 添加消息到短期记忆
     * 
     * @param sessionId   会话ID
     * @param messagePair 消息对
     */
    void addMessage(String sessionId, MessagePair messagePair);

    /**
     * 获取最近N轮对话
     * 
     * @param sessionId 会话ID
     * @param count     获取数量
     * @return 最近的消息对列表
     */
    List<MessagePair> getRecentMessages(String sessionId, int count);

    /**
     * 获取所有短期记忆消息
     * 
     * @param sessionId 会话ID
     * @return 所有消息对列表
     */
    List<MessagePair> getAllMessages(String sessionId);

    /**
     * 构建短期上下文字符串
     * 
     * @param sessionId 会话ID
     * @return 格式化的短期记忆上下文
     */
    String buildShortTermContext(String sessionId);

    /**
     * 清空会话的短期记忆
     * 
     * @param sessionId 会话ID
     */
    void clearSession(String sessionId);

    /**
     * 获取当前会话的消息数量
     * 
     * @param sessionId 会话ID
     * @return 消息数量
     */
    int getMessageCount(String sessionId);
}
