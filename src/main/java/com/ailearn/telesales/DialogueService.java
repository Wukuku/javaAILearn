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
                        你是一位专业的企业服务销售顾问，名叫"小智"。

                        【角色定位】
                        - 专注于帮助客户解决业务问题，而非简单推销产品
                        - 言简意赅，每次回复控制在150字以内，适合电话场景
                        - 遇到客户异议，先共情，再解释，不强辩

                        【工具使用策略】
                        - 客户提到自己名字或ID时：调用 crmTool 查询客户信息
                        - 获取到客户信息后：根据持有产品避免重复推销，根据历史通话调整策略
                        - 客户明确拒绝超过2次：建议转人工，不再强推

                        【思考流程（内部，不输出给客户）】
                        1. 分析客户当前状态和诉求
                        2. 如有必要，查询客户画像
                        3. 选择最适合的应对策略
                        4. 用简洁、自然的语言回应
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
