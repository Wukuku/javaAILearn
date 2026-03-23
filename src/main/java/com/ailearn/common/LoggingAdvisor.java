package com.ailearn.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;

/**
 * 自定义 Advisor —— AI 请求/响应全链路日志
 *
 * 面试亮点：
 * Spring AI 的 Advisor 类似 Spring AOP，是框架级扩展点。
 * 实现 CallAroundAdvisor 可以在每次 AI 调用前后插入自定义逻辑。
 * 常见用途：日志、限流、内容安全审计、token 统计、缓存等。
 *
 * 执行顺序由 getOrder() 决定，数值越小越先执行（类似 Spring Filter 链）。
 */
@Slf4j
public class LoggingAdvisor implements CallAroundAdvisor {

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        long startTime = System.currentTimeMillis();

        // 请求前：记录用户输入（截断过长内容）
        String userText = advisedRequest.userText();
        String preview = userText != null && userText.length() > 100
                ? userText.substring(0, 100) + "..."
                : userText;
        log.info("[AI Request] userText='{}'", preview);

        // 执行实际调用
        AdvisedResponse response = chain.nextAroundCall(advisedRequest);

        // 响应后：记录耗时 + token 用量
        long elapsed = System.currentTimeMillis() - startTime;
        try {
            Usage usage = response.response().getMetadata().getUsage();
            log.info("[AI Response] elapsed={}ms | promptTokens={} | generationTokens={} | total={}",
                    elapsed,
                    usage != null ? usage.getPromptTokens() : "N/A",
                    usage != null ? usage.getGenerationTokens() : "N/A",
                    usage != null ? usage.getTotalTokens() : "N/A");
        } catch (Exception e) {
            log.info("[AI Response] elapsed={}ms", elapsed);
        }

        return response;
    }

    @Override
    public String getName() {
        return LoggingAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保记录完整的端到端耗时
        return Integer.MIN_VALUE;
    }
}
