package com.ailearn.structured;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块三：结构化输出
 *
 * 知识点：
 * - BeanOutputConverter：让 LLM 输出直接映射为 Java 对象
 * - 原理：Spring AI 自动将 Java 类型转换为 JSON Schema，注入到 Prompt 中
 *   告诉模型按指定格式输出，然后自动解析响应
 */
@RestController
@RequestMapping("/structured")
@RequiredArgsConstructor
public class StructuredOutputController {

    private final StructuredOutputService service;

    /**
     * 让模型返回结构化的书籍信息
     * GET /structured/book?title=三体
     */
    @GetMapping("/book")
    public BookInfo getBookInfo(@RequestParam String title) {
        return service.getBookInfo(title);
    }

    /**
     * 让模型返回列表
     * GET /structured/movies?genre=科幻&count=3
     */
    @GetMapping("/movies")
    public List<MovieInfo> getMovies(
            @RequestParam String genre,
            @RequestParam(defaultValue = "3") int count) {
        return service.getMovieList(genre, count);
    }
}
