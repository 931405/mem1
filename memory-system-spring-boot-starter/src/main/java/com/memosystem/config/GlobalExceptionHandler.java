package com.memosystem.config;

import com.memosystem.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.stereotype.Component;

/**
 * 全局异常处理器
 * 统一处理应用中的异常
 */
@Slf4j
@Component("mem0GlobalExceptionHandler")
@RestControllerAdvice
@ConditionalOnMissingBean(name = "mem0GlobalExceptionHandler")
public class GlobalExceptionHandler {

    /**
     * 处理参数验证失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数验证失败");

        log.warn("参数验证失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(5000, "系统内部错误: " + e.getMessage());
    }
}
