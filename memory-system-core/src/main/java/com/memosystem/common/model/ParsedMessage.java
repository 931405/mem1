package com.memosystem.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析后的消息数据模型
 * 用于表示经过处理的用户消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParsedMessage {
    private String content;      // 消息内容
    private String language;     // 检测到的语言
    private long timestamp;      // 时间戳
    private boolean isEmpty;     // 是否为空消息
}
