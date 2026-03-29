package com.ailearn.telesales;

import com.ailearn.telesales.model.IntentAnalysis;
import com.ailearn.telesales.model.ScriptMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SimulationService 单元测试 —— 全链路 RAG-Augmented Agent
 *
 * 面试要点：
 * 1. reactor-test 的 StepVerifier：测试 Flux 流式输出
 * 2. 验证全链路编排：意图分析 → 话术检索 → 流式生成
 * 3. 验证无话术时的降级逻辑（回退到默认提示语）
 * 4. 验证 simulateAuto 会调用 IntentService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationService - 全链路 RAG-Augmented Agent")
class SimulationServiceTest {

    @Mock
    private IntentService intentService;

    @Mock
    private ScriptService scriptService;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        // Builder 链：defaultSystem → defaultAdvisors → build → prompt → user → advisors → stream → content
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(any())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        simulationService = new SimulationService(intentService, scriptService, chatClientBuilder);
    }

    @Test
    @DisplayName("simulateStream：应完整走完意图→话术→生成三步流程并返回 Flux")
    void simulateStream_shouldReturnFluxWithResponse() {
        // given
        IntentAnalysis intent = buildPriceIntent();
        List<ScriptMatch> scripts = List.of(
                new ScriptMatch("您关注利率完全正常，我们7.2%-18%，先预审？", "PRICE_INQUIRY", "ALL", 0.88)
        );
        when(scriptService.recommend(anyString(), anyString(), anyInt())).thenReturn(scripts);

        // stub 流式输出
        when(chatClient.prompt()
                .user(anyString())
                .advisors(any())
                .stream()
                .content())
                .thenReturn(Flux.just("您", "好，", "利率方面", "我来解释一下"));

        // when & then：用 StepVerifier 验证 Flux
        StepVerifier.create(simulationService.simulateStream("call-001", "你们利率多少", intent))
                .expectNext("您")
                .expectNext("好，")
                .expectNext("利率方面")
                .expectNext("我来解释一下")
                .verifyComplete();

        // 验证话术检索被调用
        verify(scriptService).recommend(eq("你们利率多少"), eq("PRICE_INQUIRY"), eq(3));
    }

    @Test
    @DisplayName("simulateStream：无匹配话术时应使用降级提示语")
    void simulateStream_shouldUseFallback_whenNoScriptsFound() {
        // given：话术库无结果
        when(scriptService.recommend(anyString(), any(), anyInt())).thenReturn(List.of());

        when(chatClient.prompt()
                .user(anyString())
                .advisors(any())
                .stream()
                .content())
                .thenReturn(Flux.just("好的，我来为您解答"));

        // when & then：不应抛异常，正常返回 Flux
        StepVerifier.create(simulationService.simulateStream("call-001", "随便问问", buildPriceIntent()))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("simulateAuto：应自动调用 IntentService 分析意图再生成回复")
    void simulateAuto_shouldCallIntentServiceFirst() {
        // given
        IntentAnalysis intent = buildPriceIntent();
        when(intentService.analyze(anyString())).thenReturn(intent);
        when(scriptService.recommend(anyString(), any(), anyInt())).thenReturn(List.of());
        when(chatClient.prompt()
                .user(anyString())
                .advisors(any())
                .stream()
                .content())
                .thenReturn(Flux.just("好的"));

        // when
        simulationService.simulateAuto("call-002", "你们利率怎么样").blockLast();

        // then：验证意图分析被自动触发
        verify(intentService).analyze("你们利率怎么样");
    }

    // ==================== 测试数据工厂 ====================

    private IntentAnalysis buildPriceIntent() {
        return new IntentAnalysis(
                "PRICE_INQUIRY",
                List.of("HESITATION"),
                "NEUTRAL",
                2,
                "HIGH",
                List.of("利率"),
                "PRICE_ANCHOR",
                "客户询问利率，价格敏感，建议价格锚定策略"
        );
    }
}
