package com.ailearn.agent;

import com.ailearn.common.LoggingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

/**
 * Agent Service —— 自主多步推理
 *
 * 面试亮点：
 * 1. Function Calling Loop：Spring AI 自动循环执行工具调用，直到模型认为任务完成
 * 2. 流式 Agent：实时输出推理过程，用户体验更好
 * 3. 带记忆的 Agent：多轮任务可以持续对话，无需重复上下文
 * 4. 强制 Chain-of-Thought：System Prompt 要求模型显式输出推理步骤
 *
 * ReAct 模式（Reasoning + Acting）：
 * 思考(Thought) → 行动(Action/Tool Call) → 观察(Observation) → 思考 → ... → 最终回答
 * Spring AI 的 Function Calling Loop 就是 ReAct 的实现
 *
 * 工具调用循环机制：
 * 模型返回 tool_calls → Spring AI 执行对应 Java Function
 * → 结果作为 ToolMessage 返回给模型 → 模型继续推理
 * → 可能再次调用工具 → 直到模型返回普通文本（任务完成）
 */
@Service
public class AgentService {

    private final ChatClient agentClient;
    private final ChatClient researchClient;

    public AgentService(ChatClient.Builder builder) {

        // ---- 旅游规划 Agent ----
        this.agentClient = builder
                .defaultSystem("""
                        你是专业的旅游规划 AI 助手。每次回答请按以下格式输出：

                        【分析】
                        （说明你的思考过程和需要用哪些工具）

                        【行动】
                        （调用工具获取所需信息）

                        【结论】
                        （基于工具结果给出完整的旅游规划）

                        工具使用原则：
                        - 必须先查询目的地天气，再制定行程
                        - 必须用计算器计算费用，不能心算
                        - 必须查询当前日期，给出准确的"几天后出发"建议
                        """)
                .defaultTools("weatherTool", "calculatorTool", "dateTimeTool")
                .defaultAdvisors(new LoggingAdvisor(), new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();

        // ---- 研究助手 Agent（专注信息综合） ----
        this.researchClient = builder
                .defaultSystem("""
                        你是专业的研究助手，擅长综合多方信息给出深度分析。
                        回答格式：
                        1. 先列出你需要了解的信息点
                        2. 调用工具逐一获取信息
                        3. 综合所有信息给出结构化分析报告
                        报告包含：摘要、详细分析、结论与建议
                        """)
                .defaultTools("weatherTool", "calculatorTool", "dateTimeTool")
                .build();
    }

    /**
     * 旅游规划（同步，等所有工具调用完成后一次性返回）
     */
    public String planTravel(String destination, int days) {
        String prompt = "请帮我规划 " + destination + " 的 " + days + " 天旅游计划。\n"
                + "需要：1）查询天气 2）每天行程安排 3）估算总费用（住宿300/晚+餐饮150/天+景点100/天）4）出行建议";
        return agentClient.prompt().user(prompt).call().content();
    }

    /**
     * 旅游规划（流式，实时输出推理过程）
     * 面试亮点：用户可以看到 Agent 思考和工具调用的实时过程
     */
    public Flux<String> streamPlanTravel(String destination, int days) {
        String prompt = "请帮我规划 " + destination + " 的 " + days + " 天旅游计划。"
                + "必须先查天气，再规划行程，再计算费用。";
        return agentClient.prompt().user(prompt).stream().content();
    }

    /**
     * 带记忆的多轮 Agent 任务
     * 支持：先规划 -> 用户反馈 -> Agent 根据反馈调整
     */
    public String chatWithAgent(String conversationId, String message) {
        return agentClient.prompt()
                .user(message)
                .advisorParam(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                .call()
                .content();
    }

    /**
     * 研究分析任务（通用 Agent）
     * 示例：analyzeTask("比较北京和上海的生活成本和宜居性")
     */
    public String analyzeTask(String task) {
        return researchClient.prompt().user(task).call().content();
    }

    public Flux<String> streamAnalyzeTask(String task) {
        return researchClient.prompt().user(task).stream().content();
    }
}
