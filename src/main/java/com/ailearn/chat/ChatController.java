package com.ailearn.chat;

import com.ailearn.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Chat Controller —— 基础对话接口
 *
 * 接口列表：
 * GET  /chat/simple          最简对话
 * GET  /chat/stream          流式输出（打字机效果）
 * GET  /chat/persona         角色扮演（TEACHER/CODE_REVIEWER/TRANSLATOR 等）
 * GET  /chat/stream/persona  流式角色扮演
 * GET  /chat/token-info      带 Token 用量统计的对话
 * GET  /chat/translate       翻译（PromptTemplate 示例）
 * POST /chat/code-review     代码审查
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/simple")
    public ApiResponse<String> simple(@RequestParam String message) {
        return ApiResponse.success(chatService.chat(message));
    }

    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam String message) {
        return chatService.streamChat(message);
    }

    /**
     * 角色扮演
     * persona 枚举值：ASSISTANT / TEACHER / CODE_REVIEWER / TRANSLATOR / PSYCHOLOGIST
     * 示例：GET /chat/persona?message=解释一下递归&persona=TEACHER
     */
    @GetMapping("/persona")
    public ApiResponse<String> persona(
            @RequestParam String message,
            @RequestParam(defaultValue = "ASSISTANT") ChatPersona persona) {
        return ApiResponse.success(chatService.chatWithPersona(message, persona));
    }

    @GetMapping(value = "/stream/persona", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> streamPersona(
            @RequestParam String message,
            @RequestParam(defaultValue = "ASSISTANT") ChatPersona persona) {
        return chatService.streamWithPersona(message, persona);
    }

    /**
     * Token 用量统计
     * 返回：回答内容 + prompt/generation/total tokens + 耗时 + 估算费用
     * 示例：GET /chat/token-info?message=介绍一下Spring AI
     */
    @GetMapping("/token-info")
    public ApiResponse<TokenUsageInfo> tokenInfo(@RequestParam String message) {
        return ApiResponse.success(chatService.chatWithTokenTracking(message));
    }

    /**
     * 翻译（PromptTemplate 示例）
     * 示例：GET /chat/translate?text=Hello World&targetLanguage=日语
     */
    @GetMapping("/translate")
    public ApiResponse<String> translate(
            @RequestParam String text,
            @RequestParam(defaultValue = "中文") String targetLanguage) {
        return ApiResponse.success(chatService.translate(text, targetLanguage));
    }

    /**
     * 代码审查
     * POST /chat/code-review
     * Body: {"code": "...", "language": "Java"}
     */
    @PostMapping("/code-review")
    public ApiResponse<String> reviewCode(@RequestBody CodeReviewRequest request) {
        return ApiResponse.success(chatService.reviewCode(request.code(), request.language()));
    }

    public record CodeReviewRequest(String code, String language) {}
}
