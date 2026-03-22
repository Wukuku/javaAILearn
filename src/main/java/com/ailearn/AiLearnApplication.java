package com.ailearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 学习项目入口
 *
 * 学习路径：
 * 1. chat      - 基础聊天（SimpleChat、流式输出）
 * 2. memory    - 多轮对话（会话记忆）
 * 3. structured - 结构化输出（BeanOutputConverter）
 * 4. tools     - Function Calling / 工具调用
 * 5. rag       - 检索增强生成（RAG）
 * 6. agent     - Agent 模式（ReAct、自主决策）
 */
@SpringBootApplication
public class AiLearnApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiLearnApplication.class, args);
    }
}
