package com.ailearn.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块二：多轮对话（会话记忆）
 *
 * 知识点：
 * - ChatMemory：Spring AI 的对话记忆抽象
 * - InMemoryChatMemory：内存实现（开发测试用）
 * - conversationId：通过 ID 隔离不同用户的对话历史
 * - 上下文窗口管理：历史消息过多时的截断策略
 */
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class MemoryChatController {

    private final MemoryChatService memoryChatService;

    /**
     * 带记忆的对话
     * conversationId 相同的请求共享同一段对话历史
     *
     * 示例：
     * GET /memory/chat?conversationId=user001&message=我叫张三
     * GET /memory/chat?conversationId=user001&message=我叫什么名字？   <- 模型能记住
     * GET /memory/chat?conversationId=user002&message=我叫什么名字？   <- 不同会话，不知道
     */
    @GetMapping("/chat")
    public String chat(
            @RequestParam String conversationId,
            @RequestParam String message) {
        return memoryChatService.chat(conversationId, message);
    }

    /**
     * 清除指定会话的历史
     * DELETE /memory/chat/user001
     */
    @DeleteMapping("/chat/{conversationId}")
    public String clearMemory(@PathVariable String conversationId) {
        memoryChatService.clearMemory(conversationId);
        return "会话 [" + conversationId + "] 历史已清除";
    }
}
