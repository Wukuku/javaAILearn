package com.ailearn.telesales;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

/**
 * 对话引擎服务
 *
 * 面试重点：三层能力叠加
 *
 * Layer 1 - Memory（多轮记忆）
 *   客户第一句说了预算，第三句问功能，Agent 能结合预算给出合适的推荐
 *   原系统：每次调用独立，无上下文，销售人员需手动记录
 *
 * Layer 2 - Tool Calling（CRM 集成）
 *   Agent 自主判断何时查询客户画像（不是每句都查，节省 API 消耗）
 *   获取：持有产品、风险等级、历史通话结果
 *   原系统：销售人员需手动查 CRM 系统，容易遗漏
 *
 * Layer 3 - Role（专业人设）
 *   System Prompt 定义销售顾问角色，控制语气、策略边界
 *   包含 Chain-of-Thought 要求：先分析客户状态，再给话术，避免盲目推销
 */
@Slf4j
@Service
public class DialogueService {

    private final ChatClient dialogueClient;

    public DialogueService(ChatClient.Builder builder) {
        this.dialogueClient = builder
                .defaultSystem("""
                        你是一位专业的信贷顾问，名叫"小慧"，代表一家持牌消费金融公司。

                        【角色定位】
                        - 帮助有资金需求的客户了解贷款产品，解答疑虑，引导申请
                        - 言简意赅，每次回复控制在120字以内，适合电话沟通
                        - 绝不夸大额度、承诺低利率；遇到投诉立刻共情并承诺跟进

                        【合规底线（必须遵守）】
                        - 不得承诺"100%通过""一定下款"等无法保证的结果
                        - 不得报出低于产品实际区间的利率来诱导客户
                        - 客户明确说不需要时，礼貌结束，不得纠缠超过2次
                        - 涉及征信查询，必须告知客户并获得同意

                        【工具使用策略】
                        - 客户提到自己名字或ID时：调用 crmTool 查询历史记录
                        - 查到历史有逾期记录：主动说明可能影响额度，避免后续纠纷
                        - 客户已有在贷产品：推荐追加额度，而非重复办理

                        【思考流程（内部，不输出给客户）】
                        1. 判断客户当前意图和情绪
                        2. 如有必要查询 CRM 了解历史
                        3. 选择合规的应对策略
                        4. 口语化、自然地回应，适合电话直接念出来
                        """)
                .defaultTools("crmTool")
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    /**
     * 同步对话
     */
    public String chat(String conversationId, String speech) {
        log.info("[对话引擎] conversationId={} speech={}", conversationId, speech);
        return dialogueClient.prompt()
                .user(speech)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .call()
                .content();
    }

    /**
     * 流式对话（推荐）
     * 流式在电销场景的价值：实时语音合成（TTS）可以边生成边播放，减少等待感
     */
    public Flux<String> streamChat(String conversationId, String speech) {
        return dialogueClient.prompt()
                .user(speech)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .stream()
                .content();
    }
}
