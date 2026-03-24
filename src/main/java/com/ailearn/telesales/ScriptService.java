package com.ailearn.telesales;

import com.ailearn.telesales.model.ScriptMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 话术知识库服务（RAG）
 *
 * 面试重点：从"规则表"到"语义检索"的升级
 *
 * 原系统：Map<Intent, List<Script>> 精确匹配
 *   问题：① 意图稍有偏差就匹配不到  ② 新业务线需要人工维护映射表
 *
 * 新方案：话术向量化入库，按语义相似度检索
 *   优势：① 模糊匹配，"价格太贵"和"费用偏高"能检索到同一批话术
 *         ② 元数据过滤：intent + segment 双维度精准过滤
 *         ③ 话术运营只需维护文档，无需改代码
 *
 * 元数据设计：
 *   type=telesales_script  → 与 RAG 模块数据隔离
 *   intent=PRICE_INQUIRY   → 按意图过滤
 *   segment=SMB            → 按客群过滤
 */
@Slf4j
@Service
public class ScriptService {

    private static final String SCRIPT_TYPE = "telesales_script";

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter(256, 50, 5, 100, false);

    public ScriptService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 添加单条话术到向量库
     *
     * @param content 话术内容
     * @param intent  对应意图标签
     * @param segment 适用客群（ALL / SMB / ENTERPRISE / INDIVIDUAL）
     */
    public void addScript(String content, String intent, String segment) {
        Document doc = new Document(content, Map.of(
                "type", SCRIPT_TYPE,
                "intent", intent,
                "segment", segment
        ));
        vectorStore.add(List.of(doc));
        log.info("[话术库] 新增话术 intent={} segment={}", intent, segment);
    }

    /**
     * 按语义 + 意图标签检索最相关话术
     *
     * @param speech  客户发言（直接用原文做语义检索，比用意图枚举效果更好）
     * @param intent  意图标签（元数据精确过滤）
     * @param topK    返回条数
     */
    public List<ScriptMatch> recommend(String speech, String intent, int topK) {
        // 构建过滤条件：必须是话术类型，可选按意图过滤
        String filter = intent != null && !intent.isBlank()
                ? "type == '" + SCRIPT_TYPE + "' && intent == '" + intent + "'"
                : "type == '" + SCRIPT_TYPE + "'";

        SearchRequest request = SearchRequest.query(speech)
                .withTopK(topK)
                .withSimilarityThreshold(0.4)   // 话术检索阈值适当放低，宁多勿少
                .withFilterExpression(filter);

        return vectorStore.similaritySearch(request).stream()
                .map(doc -> new ScriptMatch(
                        doc.getText(),
                        (String) doc.getMetadata().getOrDefault("intent", ""),
                        (String) doc.getMetadata().getOrDefault("segment", "ALL"),
                        doc.getScore() != null ? doc.getScore() : 0.0
                ))
                .toList();
    }

    /**
     * 预置话术库（首次启动或 Demo 初始化用）
     * 生产环境：从数据库 / Excel / 运营后台批量导入
     */
    public int initDefaultScripts() {
        List<Map<String, String>> scripts = List.of(
                // 价格异议话术
                Map.of("content", "我理解您对价格的关注。我们的产品定价是基于全年7×24小时的技术支持和季度功能更新。您算一下每天成本不到50元，比一杯咖啡还便宜，但带来的效率提升可以为您节省3个员工的人力。",
                        "intent", "PRICE_INQUIRY", "segment", "SMB"),
                Map.of("content", "价格方面我完全理解您的顾虑。我们现在有一个灵活的方案：首年按月付，让您零门槛体验。很多客户用了3个月后都主动转成了年付，因为确实看到了ROI。",
                        "intent", "PRICE_INQUIRY", "segment", "ALL"),
                Map.of("content", "您提到价格偏高，其实我们帮您算一笔账：您现在处理这块业务每月人力成本大概多少？我们的系统上线后通常能降低60%的人工介入，一般3-6个月就能回本。",
                        "intent", "PRICE_INQUIRY", "segment", "ENTERPRISE"),

                // 竞品对比话术
                Map.of("content", "您说得对，市面上确实有一些价格更低的产品。核心差异在于数据安全和稳定性：我们有ISO27001认证，过去两年SLA达到99.95%。如果系统宕机一天，损失的业务可能远超价格差异。",
                        "intent", "COMPETITOR_COMPARE", "segment", "ENTERPRISE"),
                Map.of("content", "竞品价格确实比我们低一些。不过很多客户从竞品迁移过来，主要原因是售后响应慢，问题积压。我们承诺4小时内响应，有专属客户成功经理，您可以先免费试用30天对比一下。",
                        "intent", "COMPETITOR_COMPARE", "segment", "SMB"),

                // 犹豫/拒绝话术
                Map.of("content", "完全理解，这种决定确实需要仔细考虑。我可以帮您准备一份详细的功能对比和客户案例，您内部讨论时可以参考。同时我们有免费的POC环境，让您的技术团队先跑起来看看效果。",
                        "intent", "HESITATION", "segment", "ENTERPRISE"),
                Map.of("content", "不着急，您多了解是对的。我发现很多犹豫的客户最后决定合作，往往是因为看了我们的成功案例。我发您3个和您同行业的案例，您看完如果还有疑虑再聊，好吗？",
                        "intent", "HESITATION", "segment", "ALL"),
                Map.of("content", "您说暂时不需要，我理解。能请教一下，现在这部分业务您是怎么处理的？我想了解一下，看看我们有没有可以帮到您的地方，不一定非得是我们的产品。",
                        "intent", "REJECTION", "segment", "ALL"),

                // 成交信号话术
                Map.of("content", "太好了！您这边我们可以本周内安排实施团队对接，最快下周就能上线。合同我这边今天就可以准备好发给您，您这边签署流程大概需要几天？",
                        "intent", "CLOSING_SIGNAL", "segment", "ALL"),
                Map.of("content", "您提到想尽快推进，正好我们12月底前签约有优惠：额外赠送3个月服务。您现在确认的话，我今天下午就安排合同和技术对接，明天就能开始部署。",
                        "intent", "CLOSING_SIGNAL", "segment", "SMB"),

                // 产品咨询话术
                Map.of("content", "我们的核心产品包含三个模块：①智能工单系统，②数据分析看板，③API开放平台。针对您这个规模的企业，通常从工单系统入手效果最明显，平均响应时间从48小时降到4小时以内。",
                        "intent", "PRODUCT_INQUIRY", "segment", "SMB"),
                Map.of("content", "我们支持私有化部署和SaaS两种模式。企业客户通常选私有化，数据完全在您自己的服务器上，满足金融/医疗等行业的合规要求。SaaS版本则免维护、自动更新，适合快速启动。",
                        "intent", "PRODUCT_INQUIRY", "segment", "ENTERPRISE")
        );

        scripts.forEach(s -> addScript(s.get("content"), s.get("intent"), s.get("segment")));
        log.info("[话术库] 预置 {} 条话术完成", scripts.size());
        return scripts.size();
    }
}
