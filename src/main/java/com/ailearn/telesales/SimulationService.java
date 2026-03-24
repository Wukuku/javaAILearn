package com.ailearn.telesales;

import com.ailearn.telesales.model.IntentAnalysis;
import com.ailearn.telesales.model.ScriptMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

/**
 * 通话模拟服务 —— 全链路编排（面试核心展示模块）
 *
 * 这个服务是整个系统的精华，展示了完整的 AI 编排能力：
 *
 * 流程：
 * ① 客户发言 (ASR输入)
 *      ↓
 * ② 意图分析（IntentService）→ 识别 primaryIntent + suggestedStrategy
 *      ↓
 * ③ 话术检索（ScriptService）→ 按意图语义检索 Top-3 参考话术
 *      ↓
 * ④ 个性化生成（ChatClient）→ 结合意图分析 + 参考话术 + 对话历史，流式生成回复
 *      ↓
 * ⑤ 实时流式输出给前端（Server-Sent Events）
 *
 * 为什么不直接用 DialogueService？
 * SimulationService 额外加入了"话术注入"步骤：
 * 先从 RAG 取出匹配话术，再把它们作为参考注入 Prompt
 * 相当于给 Agent 提供了"实时话术手册"，提升专业度 + 降低幻觉
 *
 * 这个模式在生产中称为：RAG-Augmented Agent（检索增强的 Agent）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final IntentService intentService;
    private final ScriptService scriptService;
    private final ChatClient.Builder chatClientBuilder;

    /** 每个会话一个带记忆的 Client（生产用 Redis 持久化） */
    private final InMemoryChatMemory memory = new InMemoryChatMemory();

    /**
     * 全链路流式模拟通话
     *
     * 技术亮点：意图分析（同步）→ 话术检索（同步）→ 回复生成（流式）
     * 前端可以：先显示"意图分析结果"和"参考话术"，再流式显示"AI 回复"
     *
     * @param conversationId 会话 ID（支持多轮）
     * @param speech         客户当前发言
     * @param intentAnalysis 已分析的意图（由前端先调用 /intent 获取，避免重复分析）
     */
    public Flux<String> simulateStream(String conversationId, String speech, IntentAnalysis intentAnalysis) {
        // Step 1: 检索相关话术（同步，快速）
        List<ScriptMatch> scripts = scriptService.recommend(speech, intentAnalysis.primaryIntent(), 3);
        log.info("[模拟] intent={} 检索到 {} 条话术", intentAnalysis.primaryIntent(), scripts.size());

        // Step 2: 构建注入了话术的增强 Prompt
        String scriptContext = scripts.isEmpty()
                ? "（暂无完全匹配的参考话术，请发挥专业判断）"
                : scripts.stream()
                        .map(s -> "- " + s.content())
                        .collect(Collectors.joining("\n"));

        String enrichedPrompt = """
                【当前意图分析】
                主意图：%s | 情感：%s | 建议策略：%s
                分析：%s

                【参考话术（请根据实际情况灵活运用，不要照搬）】
                %s

                【客户原话】
                "%s"

                请用不超过120字回应，语气自然，适合电话场景。
                """.formatted(
                intentAnalysis.primaryIntent(),
                intentAnalysis.sentiment(),
                intentAnalysis.suggestedStrategy(),
                intentAnalysis.summary(),
                scriptContext,
                speech
        );

        // Step 3: 流式生成（带会话记忆）
        return chatClientBuilder
                .defaultSystem("""
                        你是专业的企业服务销售顾问"小智"。
                        根据提供的意图分析和参考话术，给出自然、简洁的回应。
                        回复必须口语化，适合电话直接说出，不要有格式符号。
                        """)
                .defaultAdvisors(new MessageChatMemoryAdvisor(memory))
                .build()
                .prompt()
                .user(enrichedPrompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .stream()
                .content();
    }

    /**
     * 轻量级快速模拟（前端不传意图，服务端自动分析）
     * 适合演示用，实际生产建议拆分调用以便前端展示中间过程
     */
    public Flux<String> simulateAuto(String conversationId, String speech) {
        IntentAnalysis intent = intentService.analyze(speech);
        return simulateStream(conversationId, speech, intent);
    }
}
