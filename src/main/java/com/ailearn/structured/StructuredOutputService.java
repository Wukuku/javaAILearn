package com.ailearn.structured;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * 结构化输出 Service
 *
 * 面试亮点：
 * 1. entity(Class) 自动将 Java 类转为 JSON Schema 告知模型
 * 2. 重试机制：模型偶尔输出格式错误，自动重试并在 Prompt 中加强格式要求
 * 3. 复杂嵌套对象：ResumeInfo 多层嵌套，证明实际 NLP 场景可用性
 * 4. 泛型列表：ParameterizedTypeReference 处理 List<T>
 */
@Slf4j
@Service
public class StructuredOutputService {

    private final ChatClient chatClient;

    public StructuredOutputService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // ===================== 基础示例 =====================

    public BookInfo getBookInfo(String title) {
        return callWithRetry(() ->
                chatClient.prompt()
                        .user(u -> u.text("请介绍书籍《{title}》的详细信息，评分范围 0-10")
                                .param("title", title))
                        .call()
                        .entity(BookInfo.class),
                2
        );
    }

    public List<MovieInfo> getMovieList(String genre, int count) {
        return callWithRetry(() ->
                chatClient.prompt()
                        .user(u -> u.text("请推荐 {count} 部{genre}类型的经典电影")
                                .param("count", count)
                                .param("genre", genre))
                        .call()
                        .entity(new ParameterizedTypeReference<List<MovieInfo>>() {}),
                2
        );
    }

    // ===================== 简历解析（复杂嵌套） =====================

    /**
     * 从自由文本中提取结构化简历信息
     *
     * 真实场景：HR系统自动解析候选人简历
     * Spring AI 会将 ResumeInfo（含嵌套 record）转为多层 JSON Schema
     */
    public ResumeInfo parseResume(String resumeText) {
        return callWithRetry(() ->
                chatClient.prompt()
                        .system("""
                                你是专业的简历解析器。
                                请从候选人简历中提取结构化信息。
                                日期格式统一为 yyyy-MM，在职请填写"至今"。
                                如果某个字段在简历中没有提及，填 null。
                                """)
                        .user("请解析以下简历：\n\n" + resumeText)
                        .call()
                        .entity(ResumeInfo.class),
                3   // 简历解析较复杂，允许重试3次
        );
    }

    // ===================== 通用重试机制 =====================

    /**
     * 带重试的结构化输出调用
     *
     * 面试亮点：生产级健壮性设计
     * 问题背景：LLM 偶尔不遵守 JSON Schema，导致反序列化失败
     * 解决思路：捕获异常后重试，第二次起在 Prompt 中加强格式要求
     *
     * @param supplier   调用逻辑
     * @param maxRetries 最大重试次数
     */
    private <T> T callWithRetry(Supplier<T> supplier, int maxRetries) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("结构化输出第 {}/{} 次尝试失败: {}", attempt, maxRetries, e.getMessage());
            }
        }
        throw new RuntimeException(
                "结构化输出失败，已重试 " + maxRetries + " 次: " + lastException.getMessage(),
                lastException
        );
    }
}
