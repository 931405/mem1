package com.memosystem.common.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {

    private int code;   // 状态码
    private String message; // 消息
    private T data;     // 数据
    private Long timestamp; // 时间戳

    public static <T> Result<T> success(T data){
        return new Result<T>(0, "成功", data, System.currentTimeMillis());
    }

    public static <T> Result<T> success(String message, T data){
        return new Result<T>(0, message, data, System.currentTimeMillis());
    }

    public static <T> Result<T> error(int code, String message){
        return new Result<T>(code, message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> error(String message){
        return Result.error(500, message);
    }

    public static <T> Result<T> badRequest(String message) {
        return Result.error(1001, message);
    }

    public static <T> Result<T> businessError(String message) {
        return Result.error(3000, message);
    }
}
