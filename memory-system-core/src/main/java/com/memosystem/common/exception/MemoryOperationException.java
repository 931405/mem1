package com.memosystem.common.exception;

/**
 * 记忆操作异常
 * 当记忆的增删改查操作失败时抛出
 */
public class MemoryOperationException extends MemorySystemException {

    public MemoryOperationException(String message) {
        super(message);
    }

    public MemoryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
