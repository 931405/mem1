package com.memosystem.common.exception;

/**
 * JSON 解析异常
 * 当 JSON 解析或序列化失败时抛出
 */
public class JsonParseException extends MemorySystemException {

    public JsonParseException(String message) {
        super(message);
    }

    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
