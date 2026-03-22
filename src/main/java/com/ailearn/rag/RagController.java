package com.ailearn.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块五：RAG（Retrieval-Augmented Generation，检索增强生成）
 *
 * 知识点：
 * - 为什么需要 RAG：LLM 有知识截止日期，无法了解私有/实时数据
 * - RAG 流程：
 *   1. 知识入库：文档 -> 切片(Chunk) -> 向量化(Embedding) -> 存入向量数据库
 *   2. 检索增强：用户提问 -> 向量化 -> 相似度搜索 -> 找到相关文档片段
 *   3. 生成回答：将检索结果 + 用户问题 -> 发给 LLM -> 生成回答
 * - 核心组件：
 *   - DocumentReader：读取文档（PDF/Word/网页/代码）
 *   - TextSplitter：将文档切分为合适大小的块
 *   - EmbeddingModel：文本向量化
 *   - VectorStore：向量存储和检索
 *   - QuestionAnswerAdvisor：将检索整合进对话流程
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 添加知识到向量库
     * POST /rag/documents
     * Body: { "content": "Spring AI 是一个...", "source": "spring-ai-docs" }
     */
    @PostMapping("/documents")
    public String addDocument(@RequestBody DocumentRequest request) {
        ragService.addDocument(request.content(), request.source());
        return "文档已成功向量化并存入知识库";
    }

    /**
     * 基于知识库的问答
     * GET /rag/ask?question=Spring AI 支持哪些向量数据库？
     */
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return ragService.askWithRag(question);
    }

    public record DocumentRequest(String content, String source) {}
}
