package com.ailearn.structured;

import java.util.List;

/**
 * 结构化输出 - 书籍信息
 * Spring AI 会将此类的结构自动转换为 JSON Schema 告知模型
 */
public record BookInfo(
        String title,
        String author,
        int publishYear,
        String genre,
        String summary,
        List<String> tags,
        double rating
) {}
