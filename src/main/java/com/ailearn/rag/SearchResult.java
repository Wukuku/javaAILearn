package com.ailearn.rag;

/**
 * RAG 搜索结果（带相似度分数）
 *
 * 面试亮点：暴露相似度分数，帮助调优 RAG 质量
 * score 范围 0~1，越高代表与问题越相关
 * 生产建议：score < 0.7 的结果通常噪音较多，可提高 similarityThreshold
 */
public record SearchResult(
        String content,
        String source,
        double score,      // 余弦相似度，范围 [0, 1]
        Object metadata
) {}
