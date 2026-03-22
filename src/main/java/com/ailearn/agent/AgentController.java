package com.ailearn.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 模块六：AI Agent（智能代理）
 *
 * 知识点：
 * - 什么是 Agent：能自主感知环境、制定计划、调用工具、执行多步任务的 AI 系统
 * - ReAct 模式（Reasoning + Acting）：
 *   思考(Thought) -> 行动(Action) -> 观察(Observation) -> 循环直到完成
 * - Agent vs 普通 Chat：
 *   普通 Chat：用户问 -> 模型答（一次性）
 *   Agent：用户给目标 -> 模型分析 -> 决定用哪些工具 -> 执行 -> 观察结果 -> 继续...
 * - Spring AI Agent：通过多轮工具调用循环实现（Tool Calling Loop）
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 旅游规划 Agent
     * 能自主调用天气、计算等工具完成复杂任务
     * GET /agent/travel-plan?destination=北京&days=3
     */
    @GetMapping("/travel-plan")
    public String travelPlan(
            @RequestParam String destination,
            @RequestParam(defaultValue = "3") int days) {
        return agentService.planTravel(destination, days);
    }

    /**
     * 通用任务 Agent
     * GET /agent/task?goal=帮我规划一个学习Spring AI的30天计划
     */
    @GetMapping("/task")
    public String executeTask(@RequestParam String goal) {
        return agentService.executeTask(goal);
    }
}
