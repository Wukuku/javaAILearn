package com.ailearn.chat;

/**
 * Token 用量统计
 * 面试亮点：理解 LLM 的计费模型，有成本意识
 *
 * 主流模型计费方式（输入+输出分开计价）：
 * - GPT-4o-mini: 输入 $0.15/1M tokens, 输出 $0.6/1M tokens
 * - DeepSeek-V3: 输入 ¥0.001/1K tokens（约为 GPT-4o 的 1/50）
 */
public record TokenUsageInfo(
        String content,
        long promptTokens,
        long generationTokens,
        long totalTokens,
        long latencyMs
) {
    /**
     * 按 DeepSeek 价格估算成本（¥0.001/1K tokens）
     */
    public double estimatedCostCNY() {
        return totalTokens / 1000.0 * 0.001;
    }

    /**
     * 生成 tokens / 总 tokens，反映输出占比
     */
    public double outputRatio() {
        return totalTokens > 0 ? (double) generationTokens / totalTokens : 0;
    }
}
