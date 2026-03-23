package com.ailearn.rag;

import com.ailearn.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG Controller —— 知识库管理 + 智能问答
 *
 * 接口列表：
 * POST   /rag/documents                  添加文档（自动分片+向量化）
 * GET    /rag/documents                  列出所有文档来源
 * DELETE /rag/documents/{source}         按来源删除文档
 * GET    /rag/search                     语义检索（返回带分数的片段，用于调试）
 * GET    /rag/ask                        基于知识库问答
 * GET    /rag/ask/filtered               带来源过滤的问答
 *
 * 测试步骤：
 * 1. POST /rag/documents 添加几篇文档
 * 2. GET /rag/search?query=xxx 验证检索效果（看 score）
 * 3. GET /rag/ask?question=xxx 验证问答效果
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 添加文档到知识库
     * POST /rag/documents
     * Body: {"content": "Spring AI 是...", "source": "spring-ai-docs"}
     * 返回：生成的 Chunk 数量
     */
    @PostMapping("/documents")
    public ApiResponse<String> addDocument(@RequestBody DocumentRequest request) {
        int chunks = ragService.addDocument(request.content(), request.source());
        return ApiResponse.success("文档已入库，共生成 " + chunks + " 个向量片段");
    }

    /**
     * 列出所有文档来源及其 Chunk 数量
     * GET /rag/documents
     */
    @GetMapping("/documents")
    public ApiResponse<Map<String, Integer>> listDocuments() {
        return ApiResponse.success(ragService.listDocuments());
    }

    /**
     * 按来源删除文档
     * DELETE /rag/documents/spring-ai-docs
     */
    @DeleteMapping("/documents/{source}")
    public ApiResponse<Void> deleteDocument(@PathVariable String source) {
        boolean deleted = ragService.deleteDocument(source);
        if (!deleted) return ApiResponse.error(404, "未找到来源: " + source);
        return ApiResponse.success();
    }

    /**
     * 语义检索（调试用）
     * 直接返回相关文档片段和相似度分数，不经过 LLM 生成
     *
     * GET /rag/search?query=Spring AI支持哪些向量数据库&topK=3
     * GET /rag/search?query=xxx&source=spring-ai-docs  ← 过滤来源
     */
    @GetMapping("/search")
    public ApiResponse<List<SearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double threshold,
            @RequestParam(required = false) String source) {
        return ApiResponse.success(ragService.search(query, topK, threshold, source));
    }

    /**
     * 知识库问答
     * GET /rag/ask?question=Spring AI的RAG是如何工作的？
     */
    @GetMapping("/ask")
    public ApiResponse<String> ask(@RequestParam String question) {
        return ApiResponse.success(ragService.ask(question));
    }

    /**
     * 带来源过滤的问答（多租户知识隔离）
     * GET /rag/ask/filtered?question=xxx&source=spring-ai-docs
     */
    @GetMapping("/ask/filtered")
    public ApiResponse<String> askFiltered(
            @RequestParam String question,
            @RequestParam String source) {
        return ApiResponse.success(ragService.askWithSource(question, source));
    }

    public record DocumentRequest(String content, String source) {}
}
