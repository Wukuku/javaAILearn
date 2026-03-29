package com.ailearn.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * 面试亮点：
 * 1. 区分 Spring AI 特有异常类型，给出精准的业务提示
 * 2. NonTransientAiException：不可重试的 AI 错误（无效 API Key、参数格式错误）
 *    vs TransientAiException：可重试的临时错误（限流、超时），Spring AI 会自动重试
 * 3. 堆栈信息不暴露给前端（安全），但完整记录到日志（可追查）
 * 4. 统一响应格式，前端无需针对不同错误类型单独处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResponse.error(400, "缺少必填参数: " + e.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * Spring AI 不可重试异常：API Key 无效、模型不存在、请求格式错误等
     * 这类错误重试没有意义，直接返回 400 提示调用方检查配置
     */
    @ExceptionHandler(NonTransientAiException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleNonTransientAi(NonTransientAiException e) {
        log.error("[AI非重试错误] {}", e.getMessage());
        // 常见原因：API Key 过期、模型名称错误、输入超过 token 上限
        String userMessage = e.getMessage() != null && e.getMessage().contains("context length")
                ? "输入内容过长，请缩短文本后重试"
                : "AI 服务配置错误，请联系管理员";
        return ApiResponse.error(400, userMessage);
    }

    /**
     * 兜底异常：记录完整堆栈，返回通用提示
     * TransientAiException（限流/超时）被 Spring AI 自动重试耗尽后也会到这里
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("AI 服务异常: {}", e.getMessage(), e);
        return ApiResponse.error("AI 服务暂时不可用，请稍后重试");
    }
}
