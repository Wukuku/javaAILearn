package com.ailearn.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * ChatService - 封装 Spring AI ChatClient 的核心调用
 *
 * ChatClient 是 Spring AI 1.0 引入的高级 API（类似 RestClient 风格）
 * 底层仍使用 ChatModel，但提供了更流畅的链式调用体验
 */
@Service
public class ChatService {

    // ChatClient.Builder 由 Spring AI AutoConfiguration 自动注入
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        // 构建默认 ChatClient，可在此处设置全局系统提示词
        this.chatClient = builder
                .defaultSystem("你是一个专业的AI助手，请用简洁清晰的中文回答问题。")
                .build();
    }

    /**
     * 同步单次对话
     * prompt().user() 设置用户消息
     * call().content() 获取文本结果
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * 流式对话
     * stream() 代替 call()，返回 Flux<String>
     * 每个元素是模型输出的一个 token 片段
     */
    public Flux<String> streamChat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    /**
     * 动态设置 System Prompt
     * 覆盖默认系统提示词，实现角色扮演
     */
    public String chatWithSystem(String userMessage, String systemPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }
}
