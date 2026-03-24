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
                        你是资深电销话术分析专家，专注于金融/保险/SaaS产品的电话销售场景。

                        任务：对客户的发言进行多维度意图分析。

                        判断标准：
                        - 同一句话可能包含多个意图，请全部识别
                        - 情感判断要基于语气词、用词倾向，而非单纯字面意思
                        - 价格敏感度：直接问价格/对比竞品价格=HIGH，问功能=MEDIUM，主动了解=LOW
                        - 紧迫度：有时间限制词语("今天""马上""最后一天")=5，无时间限制=1-2
                        - 策略建议要考虑客户当前状态，而非套用固定模板

                        示例：
                        客户说："你们比XX贵多少？能打折吗？我这边预算有限。"
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
