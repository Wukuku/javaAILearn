package com.ailearn.telesales;

import com.ailearn.telesales.model.ScriptMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
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

    private static final String SCRIPT_TYPE  = "telesales_script";
    private static final String KEY_TYPE     = "type";
    private static final String KEY_INTENT   = "intent";
    private static final String KEY_SEGMENT  = "segment";
    private static final String KEY_CONTENT  = "content";

    private final VectorStore vectorStore;

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
        String filterExpr = (intent != null && !intent.isBlank())
                ? "type == '" + SCRIPT_TYPE + "' && intent == '" + intent + "'"
                : "type == '" + SCRIPT_TYPE + "'";

        SearchRequest request = SearchRequest.builder()
                .query(speech)
                .topK(topK)
                .similarityThreshold(0.4)
                .filterExpression(filterExpr)
                .build();

        List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(request);
        if (results == null) return List.of();
        return results.stream()
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

                // ===== 利率/费率异议 =====
                Map.of("content", "您关注利率这个完全正常。我们的年化利率在7.2%到18%之间，根据您的资质情况浮动。" +
                        "我这边先帮您做个预审，只需要几个基本信息，出来之后给您一个精准的报价，您看怎么样？",
                        "intent", "PRICE_INQUIRY", "segment", "INDIVIDUAL"),
                Map.of("content", "我理解您觉得利率偏高。坦白说市场上利率更低的产品通常要求资质更好或抵押物。" +
                        "我们的优势是审批快、无抵押、放款最快当天到账。您现在资金需求比较急吗？",
                        "intent", "PRICE_INQUIRY", "segment", "ALL"),
                Map.of("content", "关于费率我们完全透明，没有任何隐藏费用。您每借1万元，每天的利息大概是2到5块钱。" +
                        "您大概需要多少额度、借多少时间？我帮您算一下总费用。",
                        "intent", "PRICE_INQUIRY", "segment", "ALL"),

                // ===== 竞品对比 =====
                Map.of("content", "其他平台的利率确实有标得低的，但要注意看清楚是月息还是年息，以及有没有服务费、管理费。" +
                        "我们所有费用都体现在合同里，您签之前可以仔细看，没有任何套路。",
                        "intent", "COMPETITOR_COMPARE", "segment", "ALL"),
                Map.of("content", "银行利率当然更低，但银行审批周期一般要15到30个工作日，而且对流水、征信要求很严。" +
                        "如果您银行那边能下来，优先选银行。如果需要快速周转，我们3个工作日内就能放款。",
                        "intent", "COMPETITOR_COMPARE", "segment", "ALL"),

                // ===== 犹豫 =====
                Map.of("content", "完全理解，借款是大事，多想想是对的。我不催您，您先了解一下我们的产品，不合适也没关系。" +
                        "您现在主要的顾虑是利率、额度、还是还款方式这方面？",
                        "intent", "HESITATION", "segment", "ALL"),
                Map.of("content", "您说再考虑考虑，我理解。我建议您先做个预审，预审不影响征信，出额度之后您再决定要不要借，怎么样？",
                        "intent", "HESITATION", "segment", "ALL"),

                // ===== 明确拒绝 =====
                Map.of("content", "好的，不打扰您了。如果后续有资金需要，欢迎随时联系我，我这边24小时都可以帮您查额度。祝您生活愉快。",
                        "intent", "REJECTION", "segment", "ALL"),
                Map.of("content", "理解，暂时不需要。能冒昧问一下，您目前是资金不缺，还是对我们产品有什么顾虑？" +
                        "如果是顾虑的话我可以帮您解释一下，如果真的不需要我就不打扰了。",
                        "intent", "REJECTION", "segment", "ALL"),

                // ===== 成交信号 =====
                Map.of("content", "太好了！我现在就帮您发起申请。您需要准备身份证和银行卡，全程线上操作，大概5分钟就能提交完。" +
                        "审批通过后我第一时间通知您，放款最快今天就到账。",
                        "intent", "CLOSING_SIGNAL", "segment", "ALL"),
                Map.of("content", "好的，我这边马上给您走流程。您的手机号是注册号码对吗？" +
                        "我发您一个申请链接，您按步骤填一下，有任何问题随时叫我。",
                        "intent", "CLOSING_SIGNAL", "segment", "ALL"),

                // ===== 产品咨询 =====
                Map.of("content", "我们主要做个人信用贷款，最高额度20万，期限3到36个月，等额还款或先息后本都可以选。" +
                        "不需要抵押、不需要担保，凭个人征信和收入流水即可申请。您大概需要多少额度？",
                        "intent", "PRODUCT_INQUIRY", "segment", "INDIVIDUAL"),
                Map.of("content", "我们针对小微企业主也有专属产品，最高50万，可以用营业执照和企业流水申请。" +
                        "很多老板用来周转货款、备货，短期用完就还，按天计息，不用的时候不收费。",
                        "intent", "PRODUCT_INQUIRY", "segment", "SMB"),

                // ===== 征信/资质顾虑 =====
                Map.of("content", "征信有瑕疵不一定就不能申请。只要不是严重逾期或者当前有在执，我们都可以帮您试一下。" +
                        "预审不上征信，试了不影响您，要不要先查一下？",
                        "intent", "HESITATION", "segment", "INDIVIDUAL"),

                // ===== 投诉/负面情绪 =====
                Map.of("content", "非常抱歉给您造成困扰。您说的情况我认真记录下来，会第一时间反馈给主管处理。" +
                        "能告诉我具体是哪个环节让您不满意吗？我们一定改进。",
                        "intent", "COMPLAINT", "segment", "ALL")
        );

        scripts.forEach(s -> addScript(s.get("content"), s.get("intent"), s.get("segment")));
        log.info("[话术库] 预置 {} 条话术完成", scripts.size());
        return scripts.size();
    }
}
