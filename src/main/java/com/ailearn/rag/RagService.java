package com.ailearn.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG Service —— 检索增强生成
 *
 * 面试亮点：
 * 1. TokenTextSplitter 文档分片：大文档切成合适大小的 Chunk，避免超出上下文窗口
 * 2. 元数据过滤：按 source、标签等字段精确过滤，不相关文档不参与检索
 * 3. 相似度分数：返回 score，直观展示检索质量，支持调优
 * 4. 文档 ID 管理：支持按来源批量删除文档
 * 5. 可调参数：topK、similarityThreshold，适应不同精度/召回率需求
 *
 * RAG 完整流程：
 * 【入库】文本 → TokenTextSplitter 分片 → EmbeddingModel 向量化 → VectorStore 存储
 * 【检索】用户问题 → 向量化 → 余弦相似度检索 → Top-K 文档片段
 * 【生成】检索结果 + 问题 → LLM → 最终答案
 */
@Slf4j
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /**
     * 文档 ID 注册表：source → [docId1, docId2, ...]
     * 用于支持按来源删除文档（VectorStore 本身只支持按 ID 删除）
     */
    private final Map<String, List<String>> documentRegistry = new ConcurrentHashMap<>();

    /**
     * TokenTextSplitter 配置说明：
     * - defaultChunkSize: 每个 Chunk 的 token 上限（512 适合大多数模型）
     * - minChunkSizeChars: Chunk 最少字符数，避免产生无意义的碎片
     * - minChunkLengthToEmbed: 太短的 Chunk 直接丢弃
     * - maxNumChunks: 单文档最多切多少 Chunk
     * - keepSeparator: 是否在 Chunk 末尾保留分隔符（保留上下文边界）
     */
    private final TokenTextSplitter splitter = new TokenTextSplitter(512, 100, 5, 1000, true);

    public RagService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultSystem("""
                        你是专业的知识问答助手。
                        请仅基于提供的参考资料回答问题，答案要准确、简洁。
                        如果参考资料中没有相关信息，请明确告知用户，不要编造答案。
                        回答时可以注明信息来源（source 字段）。
                        """)
                .defaultAdvisors(
                        new QuestionAnswerAdvisor(vectorStore,
                                SearchRequest.builder().topK(5).similarityThreshold(0.65).build())
                )
                .build();
    }

    // ===================== 文档管理 =====================

    /**
     * 添加文档（自动分片 + 向量化）
     *
     * @param content 文档内容
     * @param source  来源标识，用于过滤和管理
     */
    public int addDocument(String content, String source) {
        Document doc = new Document(content, Map.of("source", source, "timestamp", System.currentTimeMillis()));

        // TokenTextSplitter 将长文档切成多个 Chunk
        List<Document> chunks = splitter.apply(List.of(doc));

        // 记录每个 Chunk 的 ID，便于后续按 source 删除
        List<String> ids = chunks.stream().map(Document::getId).toList();
        documentRegistry.merge(source, new ArrayList<>(ids), (oldIds, newIds) -> {
            oldIds.addAll(newIds);
            return oldIds;
        });

        vectorStore.add(chunks);
        log.info("文档 [{}] 已分为 {} 个 Chunk 存入向量库", source, chunks.size());
        return chunks.size();
    }

    /**
     * 按来源删除文档（删除该 source 对应的所有 Chunk）
     */
    public boolean deleteDocument(String source) {
        List<String> ids = documentRegistry.remove(source);
        if (ids == null || ids.isEmpty()) return false;
        vectorStore.delete(ids);
        log.info("已删除来源 [{}] 的 {} 个文档片段", source, ids.size());
        return true;
    }

    /**
     * 列出所有已入库的文档来源
     */
    public Map<String, Integer> listDocuments() {
        Map<String, Integer> result = new LinkedHashMap<>();
        documentRegistry.forEach((source, ids) -> result.put(source, ids.size()));
        return result;
    }

    // ===================== 检索 =====================

    /**
     * 语义检索（不经过 LLM，直接返回相关文档片段和分数）
     * 用于调试 RAG 质量，查看检索是否准确
     *
     * @param query              检索问题
     * @param topK               返回数量
     * @param similarityThreshold 相似度阈值（0~1）
     * @param source             可选：按来源过滤
     */
    public List<SearchResult> search(String query, int topK, double similarityThreshold, String source) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        if (source != null && !source.isBlank()) {
            builder.filterExpression("source == '" + source + "'");
        }

        SearchRequest request = builder.build();

        return vectorStore.similaritySearch(request).stream()
                .map(doc -> new SearchResult(
                        doc.getText(),
                        (String) doc.getMetadata().get("source"),
                        doc.getScore() != null ? doc.getScore() : 0.0,
                        doc.getMetadata()
                ))
                .toList();
    }

    // ===================== 问答 =====================

    /**
     * 基于知识库问答（QuestionAnswerAdvisor 自动完成检索+注入）
     */
    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * 带来源过滤的问答
     * 只在指定 source 的文档中检索，实现多租户知识隔离
     */
    public String askWithSource(String question, String source) {
        return ChatClient.builder(chatClient.mutate()
                        .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore,
                                SearchRequest.builder()
                                        .query(question)
                                        .topK(5)
                                        .similarityThreshold(0.65)
                                        .filterExpression("source == '" + source + "'")
                                        .build()))
                        .build())
                .build()
                .prompt()
                .user(question)
                .call()
                .content();
    }
}
