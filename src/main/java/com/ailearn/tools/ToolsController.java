package com.ailearn.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * Function Calling 演示 Controller
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolsController {

    private final ChatClient.Builder chatClientBuilder;
    private final WeatherTool weatherTool;
    private final CalculatorTool calculatorTool;

    /**
     * 天气查询 - 模型自动决定调用 WeatherTool
     * GET /tools/weather?message=北京今天天气怎么样？
     * GET /tools/weather?message=上海和深圳哪个更热？   <- 模型会调用两次
     */
    @GetMapping("/weather")
    public String weather(@RequestParam String message) {
        return chatClientBuilder.build()
                .prompt()
                .user(message)
                // 注册工具，模型会根据需要自动调用
                .functions("weatherTool")
                .call()
                .content();
    }

    /**
     * 多工具协作
     * GET /tools/multi?message=北京天气怎样？顺便算一下 123 * 456 等于多少
     */
    @GetMapping("/multi")
    public String multiTool(@RequestParam String message) {
        return chatClientBuilder.build()
                .prompt()
                .user(message)
                .functions("weatherTool", "calculatorTool")  // 注册多个工具
                .call()
                .content();
    }
}
