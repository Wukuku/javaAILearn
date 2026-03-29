package com.ailearn.telesales;

import com.ailearn.telesales.model.QualityReport;
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
 * QualityService 单元测试 —— LLM-as-Judge 模式验证
 *
 * 面试要点：
 * 1. 测试 LLM-as-Judge 的输出结构是否完整
 * 2. 验证违规场景：承诺放款 → complianceScore 应低
 * 3. 验证优质场景：合规专业 → overallGrade 应为 S/A
 * 4. 边界测试：空通话记录的处理
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QualityService - LLM-as-Judge 质检服务")
class QualityServiceTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ChatClient.Builder builder;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private QualityService qualityService;

    @BeforeEach
    void setUp() {
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        qualityService = new QualityService(builder);
    }

    @Test
    @DisplayName("assess：合规优质通话应返回 S 等级，高分，无违规")
    void assess_compliantCall_shouldReturnHighGrade() {
        // given：模拟合规通话的质检结果
        QualityReport expected = new QualityReport(
                95,     // complianceScore
                88,     // professionalismScore
                90,     // customerExperienceScore
                0.72,   // conversionProbability
                List.of(), // violations 为空
                List.of("主动告知征信影响", "利率表述准确", "尊重客户决定"),
                List.of("可以更主动询问客户具体资金需求"),
                "S",    // overallGrade
                "通话合规专业，客户体验好，有较强转化可能性"
        );

        when(chatClient.prompt()
                .user(any(java.util.function.Consumer.class))
                .call()
                .entity(QualityReport.class))
                .thenReturn(expected);

        // when
        QualityReport result = qualityService.assess(buildCompliantTranscript());

        // then
        assertThat(result.overallGrade()).isEqualTo("S");
        assertThat(result.complianceScore()).isGreaterThanOrEqualTo(90);
        assertThat(result.violations()).isEmpty();
        assertThat(result.conversionProbability()).isBetween(0.0, 1.0);
        assertThat(result.strengths()).isNotEmpty();
    }

    @Test
    @DisplayName("assess：违规承诺放款应返回低合规分 + 违规记录")
    void assess_violationCall_shouldReturnLowComplianceScore() {
        // given：模拟有违规承诺的通话质检结果
        QualityReport expected = new QualityReport(
                42,     // complianceScore 低
                70,     // professionalismScore
                55,     // customerExperienceScore
                0.30,   // conversionProbability 低
                List.of(
                        "违规：'我保证您100%能通过' - 不得承诺放款结果（-30分）",
                        "违规：'利率只有5%' - 低报实际利率区间（-25分）"
                ),
                List.of("表达流畅，态度积极"),
                List.of("严禁承诺放款结果", "利率必须报区间值7.2%-18%", "建议参加合规培训"),
                "D",    // overallGrade
                "存在严重合规风险，承诺放款和虚报利率，需立即整改"
        );

        when(chatClient.prompt()
                .user(any(java.util.function.Consumer.class))
                .call()
                .entity(QualityReport.class))
                .thenReturn(expected);

        // when
        QualityReport result = qualityService.assess(buildViolationTranscript());

        // then
        assertThat(result.overallGrade()).isIn("C", "D");
        assertThat(result.complianceScore()).isLessThan(60);
        assertThat(result.violations()).isNotEmpty();
        assertThat(result.violations()).anyMatch(v -> v.contains("违规"));
        assertThat(result.suggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("assess：转化概率应在合法范围 0.0~1.0 内")
    void assess_conversionProbability_shouldBeInValidRange() {
        // given
        QualityReport expected = new QualityReport(
                75, 80, 78, 0.45, List.of(), List.of(), List.of(), "B", "通话质量合格"
        );

        when(chatClient.prompt()
                .user(any(java.util.function.Consumer.class))
                .call()
                .entity(QualityReport.class))
                .thenReturn(expected);

        // when
        QualityReport result = qualityService.assess("销售：您好\n客户：说一下");

        // then
        assertThat(result.conversionProbability())
                .isGreaterThanOrEqualTo(0.0)
                .isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("assess：质检报告所有必填字段不应为 null")
    void assess_allRequiredFieldsShouldBePresent() {
        // given
        QualityReport expected = new QualityReport(
                80, 82, 78, 0.55,
                List.of(),
                List.of("适时沉默给客户思考空间"),
                List.of("建议更早问清客户资金用途"),
                "A",
                "整体表现良好"
        );

        when(chatClient.prompt()
                .user(any(java.util.function.Consumer.class))
                .call()
                .entity(QualityReport.class))
                .thenReturn(expected);

        // when
        QualityReport result = qualityService.assess("销售：您好\n客户：你好");

        // then：所有字段都有值
        assertThat(result.overallGrade()).isNotBlank();
        assertThat(result.summary()).isNotBlank();
        assertThat(result.violations()).isNotNull();
        assertThat(result.strengths()).isNotNull();
        assertThat(result.suggestions()).isNotNull();
    }

    // ==================== 测试数据工厂 ====================

    private String buildCompliantTranscript() {
        return """
                销售：您好，我是小慧，来自xx消费金融。请问您最近有资金需求吗？
                客户：有的，我需要大概10万，想了解一下。
                销售：好的，我们的额度最高可以到20万，年化利率在7.2%到18%之间，根据您的征信情况来定。
                      这边帮您做个预审，预审不会影响征信，您看方便吗？
                客户：好的可以。
                销售：感谢您，我现在需要您同意查询一下征信，这样我才能给您出具体额度，您同意吗？
                客户：同意。
                """;
    }

    private String buildViolationTranscript() {
        return """
                销售：您好，我保证您100%能通过，我们利率只有5%！
                客户：真的吗？
                销售：对的，绝对没问题，您今天就能拿到钱！
                客户：那好吧。
                """;
    }
}
