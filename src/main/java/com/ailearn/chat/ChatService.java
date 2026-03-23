package com.ailearn.chat;

import com.ailearn.common.LoggingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Chat Service —— 基础对话能力
 *
 * 面试亮点：
 * 1. LoggingAdvisor 实现全链路日志，无侵入
 * 2. PromptTemplate 模板化 Prompt，避免字符串拼接，支持变量替换
 * 3. ChatResponse 获取完整元数据（Token 用量、模型信息）
 * 4. ChatPersona 枚举实现多角色扮演
 */
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个专业的AI助手，请用简洁清晰的中文回答问题。")
                .defaultAdvisors(new LoggingAdvisor())
                .build();
    }

    // ===================== 基础对话 =====================

    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    public Flux<String> streamChat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    // ===================== 角色扮演 =====================

    /**
     * 使用预设 Persona 对话
     * 覆盖 defaultSystem，让模型扮演不同角色
     */
    public String chatWithPersona(String userMessage, ChatPersona persona) {
        return chatClient.prompt()
                .system(persona.getSystemPrompt())
                .user(userMessage)
                .call()
                .content();
    }

    public Flux<String> streamWithPersona(String userMessage, ChatPersona persona) {
        return chatClient.prompt()
                .system(persona.getSystemPrompt())
                .user(userMessage)
                .stream()
                .content();
    }

    // ===================== Token 统计 =====================

    /**
     * 带 Token 用量统计的对话
     *
     * 核心：call().chatResponse() 而非 call().content()
     * ChatResponse 包含 metadata（token用量、模型名、finish reason 等）
     */
    public TokenUsageInfo chatWithTokenTracking(String userMessage) {
        long start = System.currentTimeMillis();

        ChatResponse response = chatClient.prompt()
                .user(userMessage)
                .call()
                .chatResponse();

        long latency = System.currentTimeMillis() - start;
        String content = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();

        return new TokenUsageInfo(
                content,
                usage != null ? usage.getPromptTokens() : 0,
                usage != null ? usage.getGenerationTokens() : 0,
                usage != null ? usage.getTotalTokens() : 0,
                latency
        );
    }

    // ===================== PromptTemplate =====================

    /**
     * 翻译接口 —— PromptTemplate 示例
     *
     * PromptTemplate 优势：
     * 1. 变量替换清晰，不用字符串拼接
     * 2. 可从文件/数据库加载模板，便于运营修改
     * 3. 支持 FreeMarker 语法（条件、循环）
     */
    public String translate(String text, String targetLanguage) {
        PromptTemplate template = new PromptTemplate(
                "请将以下文本翻译成{language}，只输出翻译结果，不要任何解释：\n\n{text}"
        );
        String rendered = template.render(Map.of("language", targetLanguage, "text", text));
        return chatClient.prompt()
                .system(ChatPersona.TRANSLATOR.getSystemPrompt())
                .user(rendered)
                .call()
                .content();
    }

    /**
     * 代码审查 —— 使用 .user() 的参数化写法
     * 效果同 PromptTemplate，但更简洁（Spring AI 内置占位符替换）
     */
    public String reviewCode(String code, String language) {
        return chatClient.prompt()
                .system(ChatPersona.CODE_REVIEWER.getSystemPrompt())
                .user(u -> u.text("请审查以下 {language} 代码：\n\n```{language}\n{code}\n```")
                        .param("language", language)
                        .param("code", code))
                .call()
                .content();
    }
}
