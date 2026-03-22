package com.ailearn.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 结构化输出 Service
 *
 * Spring AI 结构化输出原理：
 * 1. 将 Java 类的字段信息转换为 JSON Schema
 * 2. 在 Prompt 末尾追加格式要求（"请按以下 JSON 格式输出..."）
 * 3. 模型返回 JSON 字符串
 * 4. Spring AI 自动用 Jackson 反序列化为 Java 对象
 */
@Service
public class StructuredOutputService {

    private final ChatClient chatClient;

    public StructuredOutputService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 返回单个对象
     * entity(BookInfo.class) 触发结构化输出转换
     */
    public BookInfo getBookInfo(String title) {
        return chatClient.prompt()
                .user(u -> u.text("请介绍书籍《{title}》的详细信息")
                        .param("title", title))
                .call()
                .entity(BookInfo.class);  // 核心：自动结构化
    }

    /**
     * 返回列表
     * 使用 ParameterizedTypeReference 处理泛型
     */
    public List<MovieInfo> getMovieList(String genre, int count) {
        return chatClient.prompt()
                .user(u -> u.text("请推荐 {count} 部{genre}类型的电影，包含片名、导演、年份和简介")
                        .param("count", count)
                        .param("genre", genre))
                .call()
                .entity(new ParameterizedTypeReference<List<MovieInfo>>() {});
    }
}
