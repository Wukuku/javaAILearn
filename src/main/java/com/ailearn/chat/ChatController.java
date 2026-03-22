package com.ailearn.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 模块一：基础聊天
 *
 * 知识点：
 * - ChatClient：Spring AI 核心客户端，统一封装各大模型调用
 * - 同步调用 vs 流式调用（SSE）
 * - Prompt 构造
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 最简单的单次对话
     * GET /chat/simple?message=你好
     */
    @GetMapping("/simple")
    public String simple(@RequestParam String message) {
        return chatService.chat(message);
    }

    /**
     * 流式输出（Server-Sent Events）
     * GET /chat/stream?message=介绍一下Spring AI
     *
     * 知识点：Flux<String> + text/event-stream 实现打字机效果
     */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam String message) {
        return chatService.streamChat(message);
    }

    /**
     * 带系统提示词的对话
     * GET /chat/with-system?message=今天天气怎么样&role=天气播报员
     *
     * 知识点：System Prompt 角色设定，控制模型行为
     */
    @GetMapping("/with-system")
    public String withSystem(
            @RequestParam String message,
            @RequestParam(defaultValue = "你是一个友好的AI助手") String role) {
        return chatService.chatWithSystem(message, role);
    }
}
