package com.ailearn.agent;

import com.ailearn.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Agent Controller
 *
 * 接口列表：
 * GET /agent/travel-plan                   旅游规划（同步）
 * GET /agent/travel-plan/stream            旅游规划（流式，实时看到推理过程）
 * GET /agent/chat                          带记忆的多轮 Agent 对话
 * GET /agent/analyze                       研究分析任务（同步）
 * GET /agent/analyze/stream                研究分析任务（流式）
 *
 * 推荐先用 stream 接口体验 Agent 自主推理的过程！
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 旅游规划（同步）
     * GET /agent/travel-plan?destination=成都&days=4
     */
    @GetMapping("/travel-plan")
    public ApiResponse<String> travelPlan(
            @RequestParam String destination,
            @RequestParam(defaultValue = "3") int days) {
        return ApiResponse.success(agentService.planTravel(destination, days));
    }

    /**
     * 旅游规划（流式）—— 推荐使用
     * 能实时看到 Agent 的推理过程：查天气 → 分析 → 计算费用 → 给出建议
     * GET /agent/travel-plan/stream?destination=成都&days=4
     */
    @GetMapping(value = "/travel-plan/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> streamTravelPlan(
            @RequestParam String destination,
            @RequestParam(defaultValue = "3") int days) {
        return agentService.streamPlanTravel(destination, days);
    }

    /**
     * 多轮 Agent 对话（带记忆）
     * 示例：
     * GET /agent/chat?conversationId=trip001&message=帮我规划北京3日游
     * GET /agent/chat?conversationId=trip001&message=预算改为2000元，重新规划一下
     */
    @GetMapping("/chat")
    public ApiResponse<String> chat(
            @RequestParam String conversationId,
            @RequestParam String message) {
        return ApiResponse.success(agentService.chatWithAgent(conversationId, message));
    }

    /**
     * 研究分析（同步）
     * GET /agent/analyze?task=分析一下北京适合旅游的季节
     */
    @GetMapping("/analyze")
    public ApiResponse<String> analyze(@RequestParam String task) {
        return ApiResponse.success(agentService.analyzeTask(task));
    }

    /**
     * 研究分析（流式）
     * GET /agent/analyze/stream?task=比较上海和深圳的城市特点
     */
    @GetMapping(value = "/analyze/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> streamAnalyze(@RequestParam String task) {
        return agentService.streamAnalyzeTask(task);
    }
}
