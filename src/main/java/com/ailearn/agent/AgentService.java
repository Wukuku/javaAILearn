package com.ailearn.agent;

import com.ailearn.tools.CalculatorTool;
import com.ailearn.tools.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.stereotype.Service;

/**
 * Agent Service - 实现自主多步推理
 *
 * Spring AI 实现 Agent 的核心机制：
 * Function Calling Loop（工具调用循环）
 *
 * 当模型返回 tool_calls 而非直接回答时：
 * 1. Spring AI 自动执行对应的 Java Function
 * 2. 将执行结果作为 ToolMessage 返回给模型
 * 3. 模型继续推理，可能再次调用工具
 * 4. 直到模型认为任务完成，返回最终回答
 *
 * 这个循环就是 Agent 的核心！
 */
@Service
public class AgentService {

    private final ChatClient agentClient;

    public AgentService(ChatClient.Builder builder,
                        WeatherTool weatherTool,
                        CalculatorTool calculatorTool) {
        this.agentClient = builder
                .defaultSystem("""
                        你是一个专业的旅游规划 AI 助手，具有以下能力：
                        1. 查询各城市天气信息
                        2. 进行数学计算（预算、距离等）
                        3. 根据天气和用户需求给出个性化旅游建议

                        请主动使用工具获取真实信息，而不是凭空猜测。
                        思考步骤：先了解目的地天气 -> 制定行程 -> 估算费用 -> 给出建议
                        """)
                // 注册所有可用工具，模型按需调用
                .defaultFunctions("weatherTool", "calculatorTool")
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    public String planTravel(String destination, int days) {
        String prompt = """
                请帮我规划 %s 的 %d 天旅游计划。
                要求：
                1. 先查询目的地天气，根据天气推荐合适的活动
                2. 规划每天的行程安排
                3. 估算大概费用（住宿 400元/晚，餐饮 150元/天，景点 200元/天）
                4. 给出行前准备建议
                """.formatted(destination, days);

        return agentClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public String executeTask(String goal) {
        return agentClient.prompt()
                .user(goal)
                .call()
                .content();
    }
}
