package com.ailearn.structured;

import java.util.List;

/**
 * 简历信息 —— 复杂嵌套结构化输出示例
 *
 * 面试亮点：证明 Spring AI 能处理多层嵌套对象，
 * 可用于：简历解析、合同信息提取、新闻事件抽取等 NLP 场景
 */
public record ResumeInfo(
        PersonalInfo personal,
        String summary,
        List<String> skills,
        List<WorkExperience> experiences,
        List<Education> educations,
        List<String> certifications
) {
    public record PersonalInfo(
            String name,
            String email,
            String phone,
            String location,
            String linkedIn
    ) {}

    public record WorkExperience(
            String company,
            String position,
            String startDate,       // 格式：yyyy-MM
            String endDate,         // 格式：yyyy-MM，在职填 "至今"
            String description,
            List<String> achievements
    ) {}

    public record Education(
            String school,
            String major,
            String degree,          // 本科/硕士/博士
            String startDate,
            String endDate
    ) {}
}
