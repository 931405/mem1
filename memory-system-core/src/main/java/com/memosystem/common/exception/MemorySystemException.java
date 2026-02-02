package com.memosystem.common.exception;

/**
 * 记忆系统基础异常
 * 所有记忆系统自定义异常的父类
 */
public class MemorySystemException extends RuntimeException {

    public MemorySystemException(String message) {
        super(message);
    }

    public MemorySystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
