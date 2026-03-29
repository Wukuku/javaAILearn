package com.ailearn.telesales;

import com.ailearn.telesales.model.IntentAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * IntentService 单元测试
 *
 * 面试要点：
 * 1. RETURNS_DEEP_STUBS：解决 ChatClient 流式 Builder 链的 mock 问题
 *    chatClient.prompt().user(...).call().entity(Class) 每一步都返回新对象
 *    用 RETURNS_DEEP_STUBS 可以一次性 stub 整条链
 * 2. 测试 Structured Output：验证 .entity() 返回值被正确传递
 * 3. 构造函数注入 Builder 的 mock 写法
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IntentService - 意图分析服务")
class IntentServiceTest {

    /**
     * RETURNS_DEEP_STUBS 关键：
     * 对 builder.defaultSystem("...").build() 这样的链式调用自动返回 mock 对象
     * 否则 defaultSystem() 返回 null，build() 会 NPE
     */
    @Mock(answer = RETURNS_DEEP_STUBS)
    private ChatClient.Builder builder;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private IntentService intentService;

    @BeforeEach
    void setUp() {
        // 让 builder 链式调用最终返回 mock chatClient
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        intentService = new IntentService(builder);
    }

    @Test
    @DisplayName("analyze：应返回 LLM 输出的结构化意图分析")
    void analyze_shouldReturnStructuredIntentAnalysis() {
        // given：构造预期的分析结果
        IntentAnalysis expected = new IntentAnalysis(
                "PRICE_INQUIRY",
                List.of("COMPETITOR_COMPARE"),
                "NEGATIVE",
                3,
                "HIGH",
                List.of("年化利率", "银行"),
                "PRICE_ANCHOR",
                "客户对利率敏感，正在比较竞品，建议使用价格锚定策略"
        );

        // stub：mock ChatClient 链式调用返回预设结果
        when(chatClient.prompt()
                .user(any(java.util.function.Consumer.class))
                .call()
                .entity(IntentAnalysis.class))
                .thenReturn(expected);

        // when
        IntentAnalysis result = intentService.analyze("你们利率比银行高多了");

        // then
        assertThat(result).isNotNull();
        assertThat(result.primaryIntent()).isEqualTo("PRICE_INQUIRY");
        assertThat(result.sentiment()).isEqualTo("NEGATIVE");
        assertThat(result.priceSensitivity()).isEqualTo("HIGH");
        assertThat(result.suggestedStrategy()).isEqualTo("PRICE_ANCHOR");
        assertThat(result.secondaryIntents()).contains("COMPETITOR_COMPARE");
        assertThat(result.urgency()).isEqualTo(3);
    }

    @Test
    @DisplayName("analyze：成交信号应识别为 CLOSING_SIGNAL 策略 CLOSE")
    void analyze_closingSignal_shouldReturnCloseStrategy() {
        // given
        IntentAnalysis expected = new IntentAnalysis(
                "CLOSING_SIGNAL",
                List.of(),
                "POSITIVE",
                4,
                "LOW",
                List.of("申请材料", "放款时间"),
                "CLOSE",
                "客户询问申请流程，是明确成交信号，立刻引导提交申请"
        );

        when(chatClient.prompt()
                .user(any(java.util.function.Consumer.class))
                .call()
                .entity(IntentAnalysis.class))
                .thenReturn(expected);

        // when
        IntentAnalysis result = intentService.analyze("需要什么材料，多久能放款？");

        // then
        assertThat(result.primaryIntent()).isEqualTo("CLOSING_SIGNAL");
        assertThat(result.suggestedStrategy()).isEqualTo("CLOSE");
        assertThat(result.sentiment()).isEqualTo("POSITIVE");
    }

    @Test
    @DisplayName("analyze：明确拒绝应识别为 REJECTION 不再推销")
    void analyze_rejection_shouldNotPush() {
        // given
        IntentAnalysis expected = new IntentAnalysis(
                "REJECTION",
                List.of(),
                "NEGATIVE",
                1,
                "LOW",
                List.of(),
                "EMPATHY",
                "客户明确拒绝，应礼貌结束通话，不得继续纠缠"
        );

        when(chatClient.prompt()
                .user(any(java.util.function.Consumer.class))
                .call()
                .entity(IntentAnalysis.class))
                .thenReturn(expected);

        // when
        IntentAnalysis result = intentService.analyze("不需要，谢谢，挂了");

        // then
        assertThat(result.primaryIntent()).isEqualTo("REJECTION");
        // 拒绝场景策略应该是共情，而不是继续推销
        assertThat(result.suggestedStrategy()).isNotEqualTo("CLOSE");
        assertThat(result.suggestedStrategy()).isNotEqualTo("SCARCITY");
    }
}
