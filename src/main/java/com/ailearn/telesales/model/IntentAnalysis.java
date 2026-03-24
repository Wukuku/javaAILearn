package com.ailearn.telesales.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * 多维意图分析结果
 *
 * 面试重点：相比原系统 BERT 单标签分类的三大升级：
 * ① 多标签复合意图：一句"你们比XX便宜吗"同时包含 PRICE_INQUIRY + COMPETITOR_COMPARE
 * ② 多维度输出：情感 / 紧迫度 / 价格敏感度 / 关键实体，原来需要多个独立模型
 * ③ 策略直出：不再需要规则引擎，LLM 直接给出 suggestedStrategy
 *
 * Spring AI 原理：
 * BeanOutputConverter 将此 Record 的字段 + @JsonPropertyDescription 注解
 * 自动转为 JSON Schema 注入 Prompt，要求模型严格按格式返回
 */
public record IntentAnalysis(

        @JsonPropertyDescription("主意图类型：" +
                "PRICE_INQUIRY(价格询问) / PRODUCT_INQUIRY(产品咨询) / " +
                "REJECTION(明确拒绝) / HESITATION(犹豫观望) / " +
                "COMPETITOR_COMPARE(竞品对比) / CLOSING_SIGNAL(成交信号) / " +
                "COMPLAINT(投诉) / OFF_TOPIC(偏离主题)")
        String primaryIntent,

        @JsonPropertyDescription("复合意图列表：同一句话中包含的其他意图，可为空列表")
        List<String> secondaryIntents,

        @JsonPropertyDescription("客户情感状态：POSITIVE / NEUTRAL / NEGATIVE / FRUSTRATED / EXCITED")
        String sentiment,

        @JsonPropertyDescription("紧迫程度：1(最低) ~ 5(最高)，判断客户当前的时间压力")
        int urgency,

        @JsonPropertyDescription("价格敏感度：HIGH(高) / MEDIUM(中) / LOW(低)")
        String priceSensitivity,

        @JsonPropertyDescription("对话中出现的关键实体：竞品名称、价格点、具体功能需求等")
        List<String> keyEntities,

        @JsonPropertyDescription("推荐销售策略：" +
                "PRICE_ANCHOR(价格锚定) / SOCIAL_PROOF(社会证明) / SCARCITY(稀缺制造) / " +
                "FEATURE_EMPHASIS(功能强调) / EMPATHY(共情倾听) / " +
                "ESCALATE(转人工升级) / CLOSE(促成成交)")
        String suggestedStrategy,

        @JsonPropertyDescription("分析摘要：一句话概括当前对话状态和建议行动，约30字")
        String summary

) {}
