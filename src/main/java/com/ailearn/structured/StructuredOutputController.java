package com.ailearn.structured;

import com.ailearn.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 结构化输出接口
 *
 * 接口列表：
 * GET  /structured/book              书籍信息
 * GET  /structured/movies            电影推荐列表
 * POST /structured/resume            简历解析（复杂嵌套对象）
 */
@RestController
@RequestMapping("/structured")
@RequiredArgsConstructor
public class StructuredOutputController {

    private final StructuredOutputService service;

    @GetMapping("/book")
    public ApiResponse<BookInfo> getBookInfo(@RequestParam String title) {
        return ApiResponse.success(service.getBookInfo(title));
    }

    @GetMapping("/movies")
    public ApiResponse<List<MovieInfo>> getMovies(
            @RequestParam String genre,
            @RequestParam(defaultValue = "3") int count) {
        return ApiResponse.success(service.getMovieList(genre, count));
    }

    /**
     * 简历解析
     * POST /structured/resume
     * Body: {"resumeText": "张三，Java工程师，5年经验..."}
     *
     * 真实场景：HR 系统自动解析候选人简历
     */
    @PostMapping("/resume")
    public ApiResponse<ResumeInfo> parseResume(@RequestBody ResumeRequest request) {
        return ApiResponse.success(service.parseResume(request.resumeText()));
    }

    public record ResumeRequest(String resumeText) {}
}
