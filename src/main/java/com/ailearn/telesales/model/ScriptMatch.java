package com.ailearn.telesales.model;

/**
 * 话术检索结果
 *
 * @param content     话术内容
 * @param intent      对应意图
 * @param segment     适用客群
 * @param score       语义相似度分数（0~1）
 */
public record ScriptMatch(
        String content,
        String intent,
        String segment,
        double score
) {}
