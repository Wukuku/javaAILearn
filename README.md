# Java AI Learn - Spring AI 大模型开发学习项目

> 从零开始，系统学习基于 Spring AI 的大模型应用开发，涵盖基础聊天、多轮对话、结构化输出、Function Calling、RAG 检索增强、AI Agent 等核心技术。

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 使用 Record、Text Block 等新特性 |
| Spring Boot | 3.4.3 | 应用框架 |
| Spring AI | 1.0.0-M6 | AI 应用开发核心框架 |
| Maven | 3.x | 依赖管理 |

---

## 快速启动

### 第一步：选择模型服务

**方案 A：OpenAI / DeepSeek（推荐）**

DeepSeek 兼容 OpenAI 协议，性价比高，修改 `application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: sk-your-deepseek-key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
```

或通过环境变量注入（推荐，避免 key 泄露）：

```bash
export OPENAI_API_KEY=sk-xxxx
export OPENAI_BASE_URL=https://api.deepseek.com
export OPENAI_MODEL=deepseek-chat
```

**方案 B：Ollama 本地模型（免费，无需网络）**

```bash
# 安装 Ollama
brew install ollama

# 拉取中文模型（约 4GB）
ollama pull qwen2.5:7b

# 启动服务（默认监听 localhost:11434）
ollama serve
```

然后修改 `application.yml` 中 `spring.ai.openai` 改用 `spring.ai.ollama` 配置段。

### 第二步：启动项目

```bash
mvn spring-boot:run
```

访问 `http://localhost:8080` 即可开始测试。

---

## 学习路径 & API 文档

### 模块一：基础聊天 `/chat`

**核心概念：** `ChatClient`、同步调用、流式输出（SSE）、System Prompt

| 接口 | 说明 | 示例 |
|------|------|------|
| `GET /chat/simple` | 最简单的单次对话 | `?message=你好` |
| `GET /chat/stream` | 流式输出（打字机效果） | `?message=介绍一下Spring AI` |
| `GET /chat/with-system` | 自定义角色（System Prompt） | `?message=今天天气&role=天气播报员` |

**关键代码：** `chat/ChatService.java`

```java
// Spring AI 核心调用方式（链式 API）
chatClient.prompt()
    .system("你是一个专业助手")  // 设置角色
    .user(userMessage)           // 用户输入
    .call()                      // 同步调用
    .content();                  // 获取文本结果

// 流式调用
chatClient.prompt()
    .user(userMessage)
    .stream()        // 切换为流式
    .content();      // 返回 Flux<String>
```

---

### 模块二：多轮对话 `/memory`

**核心概念：** `ChatMemory`、`MessageChatMemoryAdvisor`、Advisor 模式、会话隔离

| 接口 | 说明 | 示例 |
|------|------|------|
| `GET /memory/chat` | 带记忆的对话，同一 conversationId 共享历史 | `?conversationId=user001&message=我叫张三` |
| `DELETE /memory/chat/{id}` | 清除指定会话历史 | - |

**测试多轮效果：**
```bash
# 第一轮：告知名字
GET /memory/chat?conversationId=user001&message=我叫张三

# 第二轮：模型能记住
GET /memory/chat?conversationId=user001&message=我叫什么名字？

# 不同 conversationId：互相隔离
GET /memory/chat?conversationId=user002&message=我叫什么名字？  # 不知道
```

**Advisor 模式原理：**

Spring AI 的 Advisor 类似 Spring AOP，在请求/响应流程中插入横切关注点：
- `MessageChatMemoryAdvisor`：自动注入历史消息
- `QuestionAnswerAdvisor`：RAG 检索注入
- `SafeGuardAdvisor`：内容安全过滤

---

### 模块三：结构化输出 `/structured`

**核心概念：** `BeanOutputConverter`、JSON Schema 自动生成、泛型列表

| 接口 | 说明 | 示例 |
|------|------|------|
| `GET /structured/book` | 返回书籍信息对象 | `?title=三体` |
| `GET /structured/movies` | 返回电影列表 | `?genre=科幻&count=3` |

**原理：**
```
Java 类定义 → Spring AI 生成 JSON Schema → 注入 Prompt 末尾
→ 模型按格式输出 JSON → Jackson 自动反序列化 → Java 对象
```

**关键代码：** `structured/StructuredOutputService.java`

```java
// 返回单个对象
BookInfo book = chatClient.prompt()
    .user("介绍《三体》")
    .call()
    .entity(BookInfo.class);  // 自动结构化

// 返回泛型列表
List<MovieInfo> movies = chatClient.prompt()
    .user("推荐3部科幻电影")
    .call()
    .entity(new ParameterizedTypeReference<List<MovieInfo>>() {});
```

---

### 模块四：Function Calling（工具调用）`/tools`

**核心概念：** 工具注册、模型自主决策调用、多工具协作

| 接口 | 说明 | 示例 |
|------|------|------|
| `GET /tools/weather` | 天气查询（模型自动调用工具） | `?message=北京今天天气` |
| `GET /tools/multi` | 多工具协作 | `?message=北京天气怎样？算一下123*456` |

**工具：**
- `WeatherTool`：模拟天气查询（真实场景接入天气 API）
- `CalculatorTool`：精确数学计算（避免 LLM 算错）

**Function Calling 流程：**
```
用户提问
  → 模型分析：需要查天气
  → 返回 tool_calls（调用意图）
  → Spring AI 执行 WeatherTool.apply()
  → 结果返回给模型
  → 模型生成最终回答
```

**关键代码：** `config/AiConfig.java`

```java
// 将 Java Function 注册为 Spring Bean，名称即工具名
@Bean
@Description("获取指定城市的当前天气信息")
public Function<WeatherTool.Request, WeatherTool.Response> weatherTool(...) { ... }

// 调用时指定可用工具
chatClient.prompt()
    .user(message)
    .functions("weatherTool", "calculatorTool")  // 模型按需选用
    .call()
    .content();
```

---

### 模块五：RAG（检索增强生成）`/rag`

**核心概念：** `VectorStore`、`EmbeddingModel`、`QuestionAnswerAdvisor`、语义检索

| 接口 | 说明 | 示例 |
|------|------|------|
| `POST /rag/documents` | 向量化文档并存入知识库 | Body: `{"content":"...","source":"doc1"}` |
| `GET /rag/ask` | 基于知识库的问答 | `?question=Spring AI支持哪些向量库？` |

**RAG 完整流程：**

```
【知识入库】
原始文档 → 文本切片(Chunk) → 向量化(Embedding) → 存入 VectorStore

【检索问答】
用户问题 → 向量化 → 相似度检索 → Top-K 相关片段
         → 拼入 Prompt → LLM 生成答案
```

**VectorStore 选型：**

| 实现 | 适用场景 |
|------|---------|
| `SimpleVectorStore` | 学习/开发（内存，重启清空） |
| `PgVectorStore` | 生产（PostgreSQL + pgvector 插件） |
| `MilvusVectorStore` | 大规模（亿级向量） |
| `ChromaVectorStore` | 轻量化开源方案 |

---

### 模块六：AI Agent（智能代理）`/agent`

**核心概念：** ReAct 模式、工具调用循环、自主多步推理

| 接口 | 说明 | 示例 |
|------|------|------|
| `GET /agent/travel-plan` | 旅游规划（自主查天气+计算预算） | `?destination=北京&days=3` |
| `GET /agent/task` | 通用任务执行 | `?goal=帮我制定30天学习计划` |

**Agent vs 普通 Chat：**

```
普通 Chat：用户问 → 模型答（一次交互）

Agent（ReAct 模式）：
  用户给目标
    → 思考(Thought)：需要哪些信息？
    → 行动(Action)：调用天气工具
    → 观察(Observation)：北京15°C，晴天
    → 思考：需要计算费用
    → 行动(Action)：调用计算器
    → 观察：3天费用约1950元
    → 思考：信息足够了
    → 生成最终回答
```

**本质：** Function Calling Loop + 推理能力 = Agent

---

## 项目目录结构

```
javaAILearn/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/ailearn/
    │   │   ├── AiLearnApplication.java
    │   │   ├── config/
    │   │   │   └── AiConfig.java              # VectorStore + Function Bean 注册
    │   │   ├── chat/
    │   │   │   ├── ChatController.java
    │   │   │   └── ChatService.java
    │   │   ├── memory/
    │   │   │   ├── MemoryChatController.java
    │   │   │   └── MemoryChatService.java
    │   │   ├── structured/
    │   │   │   ├── StructuredOutputController.java
    │   │   │   ├── StructuredOutputService.java
    │   │   │   ├── BookInfo.java
    │   │   │   └── MovieInfo.java
    │   │   ├── tools/
    │   │   │   ├── ToolsController.java
    │   │   │   ├── WeatherTool.java
    │   │   │   └── CalculatorTool.java
    │   │   ├── rag/
    │   │   │   ├── RagController.java
    │   │   │   └── RagService.java
    │   │   └── agent/
    │   │       ├── AgentController.java
    │   │       └── AgentService.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/ailearn/
            └── AiLearnApplicationTests.java
```

---

## 扩展方向（后续学习）

- **MCP（Model Context Protocol）**：Anthropic 提出的标准化工具协议，让 AI 应用与外部服务解耦
- **多 Agent 协作**：多个 Agent 分工协作完成复杂任务（如 AutoGen 模式）
- **流式 Agent**：实时展示 Agent 推理过程
- **持久化记忆**：将 `InMemoryChatMemory` 替换为 Redis/数据库实现
- **文档加载**：使用 `PdfDocumentReader` / `TikaDocumentReader` 加载真实文档到 RAG

---

## 参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
- [DeepSeek API 文档](https://platform.deepseek.com/docs)
- [Ollama 本地模型](https://ollama.ai)
