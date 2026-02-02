package com.memosystem.core.summary;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalSummaryEntry {
    private String sessionId;   // 会话ID
    private String globalSummary; // 全局摘要内容

}
