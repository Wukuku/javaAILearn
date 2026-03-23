package com.ailearn.tools;

import com.ailearn.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * Function Calling 演示 Controller
 *
 * 接口列表：
 * GET /tools/weather      天气查询（单工具）
 * GET /tools/calculate    数学计算（单工具）
 * GET /tools/datetime     日期时间（单工具）
 * GET /tools/multi        多工具协作（模型自主决定调用哪些工具）
 *
 * Function Calling 核心原理：
 * 1. 把工具描述（@Description + 参数说明）序列化成 JSON Schema 发给模型
 * 2. 模型根据用户意图决定是否调用工具，以及填充哪些参数
 * 3. Spring AI 拦截模型的 tool_calls 响应，执行 Java 函数
 * 4. 把执行结果作为 ToolMessage 回传给模型
 * 5. 模型根据工具结果生成最终自然语言回答
 * 这个循环可以执行多轮（多个工具、多次调用）
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolsController {

    // 每次请求单独构建 ChatClient 以便动态指定工具
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 天气查询
     * GET /tools/weather?message=北京今天天气怎么样？
     * GET /tools/weather?message=上海和广州哪个更热？  ← 模型会调用两次工具
     */
    @GetMapping("/weather")
    public ApiResponse<String> weather(@RequestParam String message) {
        String result = chatClientBuilder.build()
                .prompt()
                .user(message)
                .tools("weatherTool")
                .call()
                .content();
        return ApiResponse.success(result);
    }

    /**
     * 精确计算（解决 LLM 计算不准确的问题）
     * GET /tools/calculate?message=1234567 乘以 8901234 等于多少？
     */
    @GetMapping("/calculate")
    public ApiResponse<String> calculate(@RequestParam String message) {
        String result = chatClientBuilder.build()
                .prompt()
                .user(message)
                .tools("calculatorTool")
                .call()
                .content();
        return ApiResponse.success(result);
    }

    /**
     * 日期时间查询（解决 LLM 不知道当前时间的问题）
     * GET /tools/datetime?message=现在北京时间几点？
     * GET /tools/datetime?message=从2024-01-01到今天过了多少天？
     */
    @GetMapping("/datetime")
    public ApiResponse<String> datetime(@RequestParam String message) {
        String result = chatClientBuilder.build()
                .prompt()
                .user(message)
                .tools("dateTimeTool")
                .call()
                .content();
        return ApiResponse.success(result);
    }

    /**
     * 多工具协作 —— 模型自主选择工具
     * GET /tools/multi?message=北京今天天气怎样？现在几点了？再帮我算一下 365*24
     *
     * 模型会按需调用多个工具，Spring AI 自动处理工具调用循环
     */
    @GetMapping("/multi")
    public ApiResponse<String> multiTool(@RequestParam String message) {
        String result = chatClientBuilder.build()
                .prompt()
                .user(message)
                .tools("weatherTool", "calculatorTool", "dateTimeTool")
                .call()
                .content();
        return ApiResponse.success(result);
    }
}
