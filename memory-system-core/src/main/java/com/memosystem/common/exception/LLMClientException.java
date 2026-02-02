package com.memosystem.common.exception;

/**
 * LLM 调用异常
 * 当调用 LLM API 失败时抛出
 */
public class LLMClientException extends MemorySystemException {

    public LLMClientException(String message) {
        super(message);
    }

    public LLMClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
