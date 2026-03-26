package com.ailearn.telesales;

import com.ailearn.telesales.model.IntentAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 意图分析服务
 *
 * 核心思路：用 Spring AI Structured Output 替代原系统的 BERT 分类器
 *
 * 原系统痛点：
 * 1. BERT 只能单标签分类，无法处理复合意图（"你们比竞品便宜吗" = 价格 + 竞品对比）
 * 2. 情感/紧迫度需要单独的模型，工程链路复杂
 * 3. 新意图类型需要重新标注数据 + 训练，周期长
 *
 * LLM 方案优势：
 * 1. Few-shot 即可处理新意图类型，无需重训练
 * 2. 多维度输出：意图 + 情感 + 策略 一次完成
 * 3. 解释性强：summary 字段直接给出自然语言分析
 *
 * 生产经验：在 System Prompt 中加入行业词汇表 + Few-shot 示例，准确率显著提升
 */
@Slf4j
@Service
public class IntentService {

    private final ChatClient chatClient;

    public IntentService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是资深信贷电销话术分析专家，专注于个人信用贷款、小微企业贷款的电话销售场景。

                        任务：对客户的发言进行多维度意图分析。

                        信贷场景专项说明：
                        - PRICE_INQUIRY：客户询问利率、费率、手续费、月供、总还款额等
                        - HESITATION：客户说"再想想""考虑一下""不太急"，或询问征信影响
                        - REJECTION：明确说"不需要""不借""挂了"等
                        - COMPETITOR_COMPARE：提到银行、其他贷款平台、利率对比
                        - CLOSING_SIGNAL：问"怎么申请""需要什么材料""多久到账"等
                        - COMPLAINT：提到催收骚扰、被误导、资料泄露等
                        - PRODUCT_INQUIRY：询问额度、期限、还款方式、是否需要抵押

                        判断标准：
                        - 价格敏感度HIGH：直接问利率/费用/和别家比价
                        - 紧迫度5：说"急用钱""今天就要""马上要周转"
                        - 策略ESCALATE：客户明确投诉或情绪激动时使用

                        示例：
                        客户说："你们利率多少？比银行高太多了吧，我再看看其他的。"
                        → primaryIntent: PRICE_INQUIRY
                        → secondaryIntents: [COMPETITOR_COMPARE, HESITATION]
                        → sentiment: NEGATIVE
                        → priceSensitivity: HIGH
                        → suggestedStrategy: PRICE_ANCHOR
                        """)
                .build();
    }

    /**
     * 分析客户发言意图
     *
     * @param speech 客户原始发言文本（来自 ASR 转写）
     * @return 多维意图分析结果
     */
    public IntentAnalysis analyze(String speech) {
        log.info("[意图分析] 输入: {}", speech);

        IntentAnalysis result = chatClient.prompt()
                .user(u -> u.text("""
                        请分析以下客户发言：

                        "{speech}"

                        输出结构化意图分析结果。
                        """)
                        .param("speech", speech))
                .call()
                .entity(IntentAnalysis.class);

        log.info("[意图分析] 主意图: {}, 策略: {}", result.primaryIntent(), result.suggestedStrategy());
        return result;
    }
}
