package com.ailearn.config;

import com.ailearn.tools.CalculatorTool;
import com.ailearn.tools.WeatherTool;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Spring AI 核心配置
 *
 * 1. VectorStore：为 RAG 模块提供内存向量数据库
 *    SimpleVectorStore 适合学习，不依赖外部服务
 *    生产环境替换为 PgVector / Milvus / Pinecone 等
 *
 * 2. Function Beans：注册工具函数供 ChatClient 使用
 *    Bean 名称即 .functions("beanName") 中使用的名称
 */
@Configuration
public class AiConfig {

    /**
     * 内存向量存储
     * EmbeddingModel 由 Spring AI AutoConfiguration 自动提供
     * （使用 OpenAI text-embedding 或本地 Transformers 模型）
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

    /**
     * 注册天气工具 Function Bean
     * @Description 注解内容是给模型看的工具说明
     */
    @Bean
    @Description("获取指定城市的当前天气信息，包括温度、天气状况和湿度")
    public Function<WeatherTool.Request, WeatherTool.Response> weatherTool(WeatherTool weatherTool) {
        return weatherTool;
    }

    /**
     * 注册计算器工具 Function Bean
     */
    @Bean
    @Description("执行基础数学运算：加减乘除")
    public Function<CalculatorTool.Request, CalculatorTool.Response> calculatorTool(CalculatorTool calculatorTool) {
        return calculatorTool;
    }
}
