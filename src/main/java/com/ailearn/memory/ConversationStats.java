package com.ailearn.memory;

/**
 * 会话统计信息
 */
public record ConversationStats(
        String conversationId,
        int totalMessages,
        long userMessages,
        long assistantMessages,
        String estimatedTokens  // 粗略估算（每个汉字约1.5 tokens，每个英文单词约1 token）
) {}
