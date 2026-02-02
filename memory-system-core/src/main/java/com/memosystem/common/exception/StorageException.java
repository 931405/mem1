package com.memosystem.common.exception;

/**
 * 存储异常
 * 当向量数据库操作失败时抛出
 */
public class StorageException extends MemorySystemException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
