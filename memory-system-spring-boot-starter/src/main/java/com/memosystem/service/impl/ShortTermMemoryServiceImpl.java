package com.memosystem.service.impl;

import com.memosystem.config.MemorySystemProperties;
import com.memosystem.core.conversation.MessagePair;
import com.memosystem.service.ShortTermMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 短期记忆服务实现
 * 使用内存缓存管理会话级别的临时记忆
 * 
 * 特点：
 * - 线程安全
 * - 自动淘汰超过最大容量的旧消息
 * - 支持 TTL 过期（可选）
 */
@Service("mem0ShortTermMemoryService")
@Slf4j
public class ShortTermMemoryServiceImpl implements ShortTermMemoryService {

    private final Map<String, SessionMemory> sessionMemories = new ConcurrentHashMap<>();

    @Autowired
    private MemorySystemProperties memoryConfig;

    /**
     * 会话内存容器
     */
    private static class SessionMemory {
        private final List<MessagePair> messages = new ArrayList<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private long lastAccessTime = System.currentTimeMillis();
        private final int maxSize;

        public SessionMemory(int maxSize) {
            this.maxSize = maxSize;
        }

        public void addMessage(MessagePair messagePair) {
            lock.writeLock().lock();
            try {
                messages.add(messagePair);
                lastAccessTime = System.currentTimeMillis();

                // 超过最大容量时，淘汰最早的消息
                while (messages.size() > maxSize) {
                    messages.remove(0);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public List<MessagePair> getRecentMessages(int count) {
            lock.readLock().lock();
            try {
                lastAccessTime = System.currentTimeMillis();
                int size = messages.size();
                int start = Math.max(0, size - count);
                return new ArrayList<>(messages.subList(start, size));
            } finally {
                lock.readLock().unlock();
            }
        }

        public List<MessagePair> getAllMessages() {
            lock.readLock().lock();
            try {
                lastAccessTime = System.currentTimeMillis();
                return new ArrayList<>(messages);
            } finally {
                lock.readLock().unlock();
            }
        }

        public void clear() {
            lock.writeLock().lock();
            try {
                messages.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int size() {
            lock.readLock().lock();
            try {
                return messages.size();
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    @Override
    public void addMessage(String sessionId, MessagePair messagePair) {
        SessionMemory memory = sessionMemories.computeIfAbsent(
                sessionId,
                k -> new SessionMemory(memoryConfig.getShortTermMemorySize()));
        memory.addMessage(messagePair);
        log.debug("短期记忆已添加，sessionId: {}, 当前容量: {}", sessionId, memory.size());
    }

    @Override
    public List<MessagePair> getRecentMessages(String sessionId, int count) {
        SessionMemory memory = getOrLoadSession(sessionId);
        if (memory == null) {
            return Collections.emptyList();
        }
        return memory.getRecentMessages(count);
    }

    @Override
    public List<MessagePair> getAllMessages(String sessionId) {
        SessionMemory memory = getOrLoadSession(sessionId);
        if (memory == null) {
            return Collections.emptyList();
        }
        return memory.getAllMessages();
    }

    /**
     * 获取或懒加载会话记忆
     * 如果内存中没有，从 messages.json 加载
     */
    private SessionMemory getOrLoadSession(String sessionId) {
        SessionMemory memory = sessionMemories.get(sessionId);
        if (memory == null) {
            // 懒加载：从持久化存储加载
            memory = loadFromPersistence(sessionId);
        }
        return memory;
    }

    /**
     * 从 messages.json 加载历史消息到短期缓存
     */
    private SessionMemory loadFromPersistence(String sessionId) {
        try {
            List<MessagePair> messages = com.memosystem.common.model.CommonFileRepository
                    .loadRecentMessages(sessionId, "messages.json",
                            memoryConfig.getShortTermMemorySize(), MessagePair.class);

            if (messages.isEmpty()) {
                return null;
            }

            SessionMemory memory = new SessionMemory(memoryConfig.getShortTermMemorySize());
            for (MessagePair msg : messages) {
                memory.addMessage(msg);
            }

            sessionMemories.put(sessionId, memory);
            return memory;

        } catch (Exception e) {
            log.warn("懒加载短期记忆失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String buildShortTermContext(String sessionId) {
        List<MessagePair> messages = getAllMessages(sessionId);
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("【最近对话历史】\n");

        for (int i = 0; i < messages.size(); i++) {
            MessagePair msg = messages.get(i);
            context.append(String.format("对话 %d:\n", i + 1));
            context.append("  用户: ").append(truncate(msg.getUserMessage(), 200)).append("\n");
            context.append("  AI: ").append(truncate(msg.getAiResponse(), 200)).append("\n");
        }

        return context.toString();
    }

    @Override
    public void clearSession(String sessionId) {
        SessionMemory memory = sessionMemories.remove(sessionId);
        if (memory != null) {
            memory.clear();
            log.info("短期记忆已清空，sessionId: {}", sessionId);
        }
    }

    @Override
    public int getMessageCount(String sessionId) {
        SessionMemory memory = sessionMemories.get(sessionId);
        return memory != null ? memory.size() : 0;
    }

    /**
     * 截断过长的文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
