package com.ailearn.telesales;

import com.ailearn.common.ApiResponse;
import com.ailearn.telesales.model.CallSession;
import com.ailearn.telesales.model.IntentAnalysis;
import com.ailearn.telesales.model.QualityReport;
import com.ailearn.telesales.model.ScriptMatch;
import com.ailearn.telesales.tool.CrmTool;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 智能电销 AI Agent 平台 —— 统一入口
 *
 * 接口列表：
 *
 * [意图分析]
 * POST /telesales/intent?speech={text}         多维意图分析（替代 BERT）
 *
 * [话术知识库]
 * POST /telesales/scripts/init                 初始化预置话术（Demo 用）
 * POST /telesales/scripts                      添加自定义话术
 * GET  /telesales/scripts/recommend            按意图推荐话术（RAG 检索）
 *
 * [客户画像]
 * GET  /telesales/customer/{id}                查询客户 CRM 信息
 *
 * [对话引擎]
 * GET  /telesales/dialogue                     同步多轮对话
 * GET  /telesales/dialogue/stream              流式多轮对话（推荐）
 *
 * [质检评分]
 * POST /telesales/quality/assess               通话质检（LLM-as-Judge）
 *
 * [通话模拟]
 * POST /telesales/simulation/stream            全链路流式模拟（核心演示接口）
 * POST /telesales/simulation/auto              自动分析意图并模拟（简化版）
 *
 * 推荐演示顺序：
 * 1. /scripts/init 初始化话术库
 * 2. /intent 查看意图分析效果
 * 3. /simulation/stream 体验全链路 Agent
 * 4. /quality/assess 体验 LLM 质检
 */
@RestController
@RequestMapping("/telesales")
@RequiredArgsConstructor
public class TeleSalesController {

    private final IntentService intentService;
    private final ScriptService scriptService;
    private final DialogueService dialogueService;
    private final QualityService qualityService;
    private final SimulationService simulationService;
    private final CallSessionManager sessionManager;

    // ==================== 意图分析 ====================

    /**
     * 多维意图分析（核心升级点，替代原 BERT 单标签分类）
     * POST /telesales/intent?speech=你们比竞品贵多少，我预算有限
     */
    @PostMapping("/intent")
    public ApiResponse<IntentAnalysis> analyzeIntent(@RequestParam String speech) {
        return ApiResponse.success(intentService.analyze(speech));
    }

    // ==================== 话术知识库 ====================

    /**
     * 初始化预置话术库（首次使用必须先调用）
     * POST /telesales/scripts/init
     */
    @PostMapping("/scripts/init")
    public ApiResponse<String> initScripts() {
        int count = scriptService.initDefaultScripts();
        return ApiResponse.success("预置话术库初始化完成，共 " + count + " 条话术");
    }

    /**
     * 添加自定义话术
     * POST /telesales/scripts
     * Body: {"content":"话术内容","intent":"PRICE_INQUIRY","segment":"SMB"}
     */
    @PostMapping("/scripts")
    public ApiResponse<Void> addScript(@RequestBody ScriptRequest request) {
        scriptService.addScript(request.content(), request.intent(), request.segment());
        return ApiResponse.success();
    }

    /**
     * 按语义检索推荐话术
     * GET /telesales/scripts/recommend?speech=你们价格太贵了&intent=PRICE_INQUIRY&topK=3
     */
    @GetMapping("/scripts/recommend")
    public ApiResponse<List<ScriptMatch>> recommendScripts(
            @RequestParam String speech,
            @RequestParam(required = false) String intent,
            @RequestParam(defaultValue = "3") int topK) {
        return ApiResponse.success(scriptService.recommend(speech, intent, topK));
    }

    // ==================== 客户画像 ====================

    /**
     * 查询客户 CRM 信息（模拟数据：C001 ~ C004）
     * GET /telesales/customer/C001
     */
    @GetMapping("/customer/{customerId}")
    public ApiResponse<CrmTool.Response> getCustomer(@PathVariable String customerId) {
        CrmTool crmTool = new CrmTool();
        return ApiResponse.success(crmTool.apply(new CrmTool.Request(customerId)));
    }

    // ==================== 对话引擎 ====================

    /**
     * 同步多轮对话（带 CRM Tool + 记忆）
     * GET /telesales/dialogue?conversationId=call-001&speech=我叫张先生，客户ID是C001
     */
    @GetMapping("/dialogue")
    public ApiResponse<String> dialogue(
            @RequestParam String conversationId,
            @RequestParam String speech) {
        return ApiResponse.success(dialogueService.chat(conversationId, speech));
    }

    /**
     * 流式多轮对话（推荐）
     * GET /telesales/dialogue/stream?conversationId=call-001&speech=你们价格怎么样
     */
    @GetMapping(value = "/dialogue/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> dialogueStream(
            @RequestParam String conversationId,
            @RequestParam String speech) {
        return dialogueService.streamChat(conversationId, speech);
    }

    // ==================== 质检评分 ====================

    /**
     * 通话质检（LLM-as-Judge 全量质检）
     * POST /telesales/quality/assess
     * Body: {"transcript":"销售：您好...\n客户：价格太贵了...","agentId":"agent-001"}
     */
    @PostMapping("/quality/assess")
    public ApiResponse<QualityReport> assessQuality(@RequestBody QualityRequest request) {
        return ApiResponse.success(qualityService.assess(request.transcript()));
    }

    // ==================== 全链路模拟 ====================

    /**
     * 全链路流式模拟（核心演示接口）
     * 流程：意图分析 → 话术检索 → 个性化回复生成（流式）
     *
     * POST /telesales/simulation/stream
     * Body: {"conversationId":"demo-001","speech":"你们比xx便宜吗","intent":{...}}
     */
    @PostMapping(value = "/simulation/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> simulateStream(@RequestBody SimulationRequest request) {
        if (request.intent() != null) {
            return simulationService.simulateStream(
                    request.conversationId(), request.speech(), request.intent());
        }
        return simulationService.simulateAuto(request.conversationId(), request.speech());
    }

    // ==================== 会话管理 ====================

    /**
     * 创建通话会话（通话开始时调用）
     * GET /telesales/session/start?conversationId=call-001&customerId=C001
     */
    @PostMapping("/session/start")
    public ApiResponse<Map<String, Object>> startSession(
            @RequestParam String conversationId,
            @RequestParam(required = false, defaultValue = "unknown") String customerId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId 不能为空");
        }
        CallSession session = sessionManager.getOrCreate(conversationId, customerId);
        return ApiResponse.success(Map.of(
                "conversationId", session.conversationId(),
                "customerId", session.customerId(),
                "status", session.status().name(),
                "startTime", session.startTime().toString()
        ));
    }

    /**
     * 结束通话会话
     * POST /telesales/session/end?conversationId=call-001&status=ENDED
     */
    @PostMapping("/session/end")
    public ApiResponse<Map<String, Object>> endSession(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "ENDED") String status) {
        CallSession.CallStatus callStatus;
        try {
            callStatus = CallSession.CallStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status 只支持：ENDED / ABANDONED");
        }
        sessionManager.endSession(conversationId, callStatus);
        return sessionManager.find(conversationId)
                .map(s -> ApiResponse.success(Map.of(
                        "conversationId", s.conversationId(),
                        "status", s.status().name(),
                        "totalTurns", s.turnCount().get(),
                        "durationSeconds", s.durationSeconds()
                )))
                .orElse(ApiResponse.error(404, "会话不存在: " + conversationId));
    }

    /**
     * 查看系统监控统计（活跃通话数、已结束数等）
     * GET /telesales/session/stats
     */
    @GetMapping("/session/stats")
    public ApiResponse<Map<String, Long>> sessionStats() {
        return ApiResponse.success(sessionManager.stats());
    }

    // ==================== Request Records ====================

    public record ScriptRequest(String content, String intent, String segment) {}

    public record QualityRequest(String transcript, String agentId) {}

    public record SimulationRequest(
            String conversationId,
            String speech,
            IntentAnalysis intent   // 可选，为 null 时服务端自动分析
    ) {}
}
