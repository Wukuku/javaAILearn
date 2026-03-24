package com.ailearn.telesales.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * CRM 客户查询工具（模拟）
 *
 * 面试重点：Function Calling 在电销场景的价值
 * 原系统：话术是静态的，无法根据客户历史定制
 * 升级后：Agent 自主决策何时查询 CRM，动态获取：
 *   - 客户风险等级 → 决定是否做风险提示
 *   - 持有产品 → 避免推销已有产品
 *   - 历史通话结果 → 调整沟通策略（避开上次拒绝点）
 *   - 通话次数 → 判断是否已疲劳，该升级人工
 *
 * 生产环境：替换 MOCK_DB 为实际 CRM API / 数据库查询
 */
@Component
public class CrmTool implements Function<CrmTool.Request, CrmTool.Response> {

    /** 模拟 CRM 数据库 */
    private static final Map<String, Response> MOCK_DB = Map.of(
            "C001", new Response("C001", "张先生", "SMB", "MEDIUM", 78.5,
                    List.of("基础版套餐"), 3, "INTERESTED", "上次对价格有顾虑，倾向于分期付款"),
            "C002", new Response("C002", "李女士", "ENTERPRISE", "LOW", 95.0,
                    List.of("旗舰版", "增值服务包"), 1, "NO_ANSWER", "决策人，需要发送正式报价"),
            "C003", new Response("C003", "王先生", "INDIVIDUAL", "HIGH", 62.0,
                    List.of(), 8, "REJECTED", "多次拒绝，主要顾虑是价格偏高"),
            "C004", new Response("C004", "赵女士", "SMB", "LOW", 88.0,
                    List.of("标准版"), 2, "INTERESTED", "有升级旗舰版意向，对新功能感兴趣")
    );

    @JsonClassDescription("CRM客户查询请求")
    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("客户ID，格式如 C001、C002")
            String customerId
    ) {}

    public record Response(
            String customerId,
            String name,
            @JsonPropertyDescription("客户分类：ENTERPRISE(企业)/SMB(中小企业)/INDIVIDUAL(个人)")
            String segment,
            @JsonPropertyDescription("风险等级：HIGH/MEDIUM/LOW")
            String riskLevel,
            @JsonPropertyDescription("信用评分 0-100")
            double creditScore,
            @JsonPropertyDescription("当前持有产品列表")
            List<String> ownedProducts,
            @JsonPropertyDescription("历史通话次数")
            int callCount,
            @JsonPropertyDescription("上次通话结果：INTERESTED/REJECTED/NO_ANSWER/CLOSED")
            String lastCallResult,
            @JsonPropertyDescription("备注：销售人员记录的关键信息")
            String notes
    ) {}

    @Override
    public Response apply(Request request) {
        return MOCK_DB.getOrDefault(request.customerId(),
                new Response(request.customerId(), "未知客户", "INDIVIDUAL", "MEDIUM",
                        70.0, List.of(), 0, "NO_ANSWER", "新客户，无历史记录"));
    }
}
