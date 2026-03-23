package com.ailearn.memory;

import com.ailearn.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Memory Chat Controller —— 多轮对话接口
 *
 * 接口列表：
 * GET    /memory/chat                          带记忆的对话
 * GET    /memory/history/{conversationId}      获取完整对话历史
 * GET    /memory/stats/{conversationId}        获取会话统计
 * DELETE /memory/chat/{conversationId}         清除会话历史
 *
 * 测试多轮效果：
 *   1. GET /memory/chat?conversationId=u001&message=我叫张三，是一名Java工程师
 *   2. GET /memory/chat?conversationId=u001&message=我叫什么名字，做什么的？  ← 能记住
 *   3. GET /memory/chat?conversationId=u002&message=我叫什么名字？           ← 不知道（不同会话）
 */
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class MemoryChatController {

    private final MemoryChatService memoryChatService;

    /**
     * 带记忆的对话
     * historySize 控制携带多少条历史（越多越消耗 tokens）
     */
    @GetMapping("/chat")
    public ApiResponse<String> chat(
            @RequestParam String conversationId,
            @RequestParam String message,
            @RequestParam(defaultValue = "20") int historySize) {
        return ApiResponse.success(memoryChatService.chat(conversationId, message, historySize));
    }

    /**
     * 获取完整对话历史
     * 返回：[{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
     */
    @GetMapping("/history/{conversationId}")
    public ApiResponse<List<Map<String, String>>> history(@PathVariable String conversationId) {
        return ApiResponse.success(memoryChatService.getHistory(conversationId));
    }

    /**
     * 会话统计
     * 返回：消息总数、用户消息数、AI 消息数、估算 token 数
     */
    @GetMapping("/stats/{conversationId}")
    public ApiResponse<ConversationStats> stats(@PathVariable String conversationId) {
        return ApiResponse.success(memoryChatService.getStats(conversationId));
    }

    @DeleteMapping("/chat/{conversationId}")
    public ApiResponse<Void> clear(@PathVariable String conversationId) {
        memoryChatService.clearMemory(conversationId);
        return ApiResponse.success();
    }
}
