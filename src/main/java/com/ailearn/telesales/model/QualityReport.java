package com.ailearn.telesales.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * 通话质检报告
 *
 * 面试重点：LLM-as-Judge 模式
 * 传统质检：人工抽检，成本高、覆盖率低（通常 < 5%）
 * LLM质检：全量覆盖，结构化输出可直接写入数据库，与BI系统集成
 *
 * 评分维度设计说明：
 * - complianceScore：监管必需，防止虚假承诺 / 违规话术
 * - professionalismScore：培训优化用，定向找出知识盲区
 * - customerExperienceScore：NPS 相关，减少强推引起的负评
 * - conversionProbability：业务核心，接入预测模型训练数据
 */
public record QualityReport(

        @JsonPropertyDescription("合规评分 0-100：话术是否合规，有无虚假承诺、违规说辞")
        int complianceScore,

        @JsonPropertyDescription("专业度评分 0-100：产品知识准确度、问题处理能力、表达流畅度")
        int professionalismScore,

        @JsonPropertyDescription("客户体验评分 0-100：是否充分倾听、避免强行推销、回应客户关切")
        int customerExperienceScore,

        @JsonPropertyDescription("预测转化概率 0.0~1.0，基于本次通话状态综合判断")
        double conversionProbability,

        @JsonPropertyDescription("违规项列表：具体说明违规内容和对应通话时间点")
        List<String> violations,

        @JsonPropertyDescription("表现亮点：值得保持和复用的优秀话术或应对方式")
        List<String> strengths,

        @JsonPropertyDescription("改进建议：具体可操作的优化点，不超过5条")
        List<String> suggestions,

        @JsonPropertyDescription("综合等级：S(优秀) / A(良好) / B(合格) / C(待改进) / D(不合格)")
        String overallGrade,

        @JsonPropertyDescription("质检总结：200字以内，包含整体评价和重点提示")
        String summary

) {}
