package com.ailearn.memory;

import com.ailearn.common.LoggingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * Memory Chat Service —— 多轮对话（带会话记忆）
 *
 * 面试亮点：
 * 1. MessageChatMemoryAdvisor 自动管理历史消息，无需手动维护
 * 2. conversationId 实现多用户会话隔离
 * 3. 提供历史记录导出、统计等管理能力
 * 4. InMemoryChatMemory 可无缝替换为 Redis/数据库实现（接口一致）
 *
 * 生产环境替换方案：
 * InMemoryChatMemory  → 开发/测试
 * CassandraChatMemory → 高并发场景
 * JdbcChatMemory      → 需要持久化（spring-ai-jdbc-store）
 */
@Service
public class MemoryChatService {

    private final ChatClient chatClient;
    private final InMemoryChatMemory chatMemory;

    public MemoryChatService(ChatClient.Builder builder) {
        this.chatMemory = new InMemoryChatMemory();
        this.chatClient = builder
                .defaultSystem("你是一个记忆力超强的AI助手，能记住对话中的所有细节，并在后续对话中主动引用。")
                .defaultAdvisors(
                        new LoggingAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    /**
     * 带记忆的对话
     *
     * @param conversationId 会话ID，相同 ID 共享历史，不同 ID 完全隔离
     * @param historySize    携带的历史消息数量上限（防止超出上下文窗口）
     */
    public String chat(String conversationId, String userMessage, int historySize) {
        return chatClient.prompt()
                .user(userMessage)
                .advisorParam(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                .advisorParam(CHAT_MEMORY_RETRIEVE_SIZE_KEY, historySize)
                .call()
                .content();
    }

    public String chat(String conversationId, String userMessage) {
        return chat(conversationId, userMessage, 20);
    }

    /**
     * 获取完整对话历史
     * 用于前端展示历史记录或导出功能
     */
    public List<Map<String, String>> getHistory(String conversationId) {
        List<Message> messages = chatMemory.get(conversationId, Integer.MAX_VALUE);
        return messages.stream()
                .filter(m -> m instanceof UserMessage || m instanceof AssistantMessage)
                .map(m -> Map.of(
                        "role", m instanceof UserMessage ? "user" : "assistant",
                        "content", m.getText()
                ))
                .toList();
    }

    /**
     * 会话统计信息
     */
    public ConversationStats getStats(String conversationId) {
        List<Message> messages = chatMemory.get(conversationId, Integer.MAX_VALUE);
        long userCount = messages.stream().filter(m -> m instanceof UserMessage).count();
        long assistantCount = messages.stream().filter(m -> m instanceof AssistantMessage).count();

        // 粗略估算 tokens（汉字约 1.5 tokens，英文单词约 1 token）
        int totalChars = messages.stream()
                .mapToInt(m -> m.getText() != null ? m.getText().length() : 0)
                .sum();
        String tokenEstimate = "约 " + (int)(totalChars * 1.5) + " tokens";

        return new ConversationStats(conversationId, messages.size(), userCount, assistantCount, tokenEstimate);
    }

    public void clearMemory(String conversationId) {
        chatMemory.clear(conversationId);
    }
}
