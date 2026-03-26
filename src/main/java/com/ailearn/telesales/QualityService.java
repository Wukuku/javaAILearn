package com.ailearn.telesales;

import com.ailearn.telesales.model.QualityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 通话质检服务
 *
 * 面试重点：LLM-as-Judge 模式的生产级应用
 *
 * 传统质检的问题：
 * - 人工抽检率通常 < 5%，大量违规话术无法被发现
 * - 评分标准主观，不同质检员结果差异大
 * - 结果是文字报告，难以量化分析 / 接入 BI 系统
 *
 * LLM 质检的价值：
 * - 全量覆盖 100% 的通话（只要有 ASR 转写）
 * - 结构化 JSON 输出，直接入库，可做聚合分析（如：哪类违规最多、哪个销售得分最低）
 * - conversionProbability 可作为特征接入转化预测模型，形成训练数据飞轮
 *
 * Prompt 工程要点：
 * - 给出具体的评分维度 + 示例，避免模型主观臆断
 * - 要求输出 violations 时附上原话，可追溯
 * - System Prompt 注入合规红线，确保一致性
 */
@Slf4j
@Service
public class QualityService {

    private final ChatClient chatClient;

    public QualityService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是专业的信贷电销质检专家，负责评估信贷顾问的通话质量。

                        【信贷行业合规红线（自动扣分项）】
                        - 承诺"100%放款""一定通过"等结果保证（-30分）
                        - 报出低于实际利率区间的利率诱导申请（-25分）
                        - 未告知客户征信查询影响直接提交申请（-20分）
                        - 客户明确拒绝后继续纠缠超过2次（-15分）
                        - 夸大额度或隐瞒费用（-25分）
                        - 使用恐吓、催促等强迫性语言（-30分）

                        【评分维度说明】
                        complianceScore：是否遵守合规红线和监管要求，满分100
                        professionalismScore：产品利率/期限/申请流程的准确度，异议处理能力
                        customerExperienceScore：是否充分倾听需求，是否尊重客户意愿，有无强推

                        【信贷场景转化概率标准】
                        - 客户主动问申请材料/流程：0.75-0.90
                        - 客户同意预审或发链接：0.55-0.75
                        - 客户表示考虑，未明确拒绝：0.25-0.45
                        - 客户明确拒绝：0.03-0.15
                        - 客户投诉或情绪激动：0.01-0.05
                        """)
                .build();
    }

    /**
     * 对完整通话记录进行质检评分
     *
     * @param transcript 通话记录，格式：
     *                   销售：xxx\n客户：xxx\n销售：xxx...
     * @return 结构化质检报告
     */
    public QualityReport assess(String transcript) {
        log.info("[质检] 开始评估，通话长度 {} 字符", transcript.length());

        QualityReport report = chatClient.prompt()
                .user(u -> u.text("""
                        请对以下通话记录进行质检评分：

                        ===通话记录===
                        {transcript}
                        ===结束===

                        请按照质检标准输出结构化评分结果。
                        violations 字段请引用原话并说明违规原因。
                        suggestions 要具体可操作，不超过5条。
                        """)
                        .param("transcript", transcript))
                .call()
                .entity(QualityReport.class);

        log.info("[质检] 完成：等级={} 合规={} 转化概率={}",
                report.overallGrade(), report.complianceScore(), report.conversionProbability());
        return report;
    }
}
