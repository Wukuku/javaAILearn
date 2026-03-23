package com.ailearn.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应封装
 * 面试亮点：所有接口统一格式，前端易对接，出错时有明确的 code + message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, System.currentTimeMillis());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "success", null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}
