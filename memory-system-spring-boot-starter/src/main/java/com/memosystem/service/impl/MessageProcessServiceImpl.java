package com.memosystem.service.impl;

import com.memosystem.common.model.ParsedMessage;
import com.memosystem.service.MessageProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息处理服务实现
 */
@Service("mem0MessageProcessService")
@Slf4j
public class MessageProcessServiceImpl implements MessageProcessService {

    @Override
    public ParsedMessage parseMessage(String userMessage) {

        if (isEmptyMessage(userMessage)) {
            return new ParsedMessage(userMessage, "unknown", System.currentTimeMillis(), true);
        }

        String cleanedMessage = cleanMessage(userMessage);
        String language = detectLanguage(userMessage);

        ParsedMessage parsed = new ParsedMessage(
                cleanedMessage,
                language,
                System.currentTimeMillis(),
                false);

        return parsed;
    }

    @Override
    public boolean isEmptyMessage(String message) {
        return message == null || message.trim().isEmpty();
    }

    @Override
    public String cleanMessage(String message) {
        if (message == null) {
            return "";
        }
        // 移除首尾空格，进行基本清理
        return message.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\p{C}]", ""); // 移除控制字符
    }

    /**
     * 检测消息的语言
     * 简单实现：根据字符集判断
     */
    private String detectLanguage(String message) {
        if (message == null || message.isEmpty()) {
            return "unknown";
        }

        int chineseCount = 0;
        int englishCount = 0;

        for (char c : message.toCharArray()) {
            if (c >= '\u4E00' && c <= '\u9FA5') {
                chineseCount++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                englishCount++;
            }
        }

        if (chineseCount > englishCount * 2) {
            return "zh-CN";
        } else if (englishCount > chineseCount * 2) {
            return "en-US";
        } else {
            return "mixed";
        }
    }
}