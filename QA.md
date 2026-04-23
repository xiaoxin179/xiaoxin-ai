# rag模型检索增强
###  检索质量

####  知识库的文档是怎么分块的？块大小是多少？块与块之间有没有重叠？

**代码依据：**

```java
// ChunkingDemoService.java - 第 14-23 行
TokenTextSplitter splitter = new TokenTextSplitter(
        512,   // 每段最大 Token 数
        100,   // 相邻段的重叠 Token 数
        5,     // 最短段落 Token 数
        10000, // 最长段落 Token 数上限
        true   // 保留原始段落元数据
);
```

**回答要点：**

| 参数 | 值 | 说明 |
|---|---|---|
| `maxNumTokens` | 512 | 每个 chunk 最大 512 tokens，这是 DashScope 模型的推荐切片粒度 |
| `minChunkSizeToEmbed` | 5 | 低于 5 tokens 的段落直接丢弃，避免噪声 |
| `overlapTokens` | 100 | 相邻 chunk 之间有 100 token 的重叠，保证语义不会在切分边界断裂 |
| `overlapForBIDIPayloadRatio` | 10000 | 长文档特殊处理 |
| `multilineEnabled` | true | 保留多行结构 |

> **追问 1：512 tokens 够用吗？**
>
> 512 tokens（约 750~1000 中文字）对大多数场景够用。如果文档是高度结构化的技术文档，可以考虑降到 256-384，减少无关上下文的引入。如果是简单 FAQ 类文档，也可以适当提高到 768。

> **追问 2：重叠 100 tokens 多不多？**
>
> 100 tokens 约等于 150-200 中文字，约占总 chunk 的 20%。这是一个合理的 trade-off——太小（<50）容易丢失边界语义，太大（>200）会造成大量重复内容、增加 token 成本。实际项目中建议根据召回质量（A/B 测试）动态调整。

> **追问 3：代码中有没有按语义/句子级别的更精细切分？**
>
> 目前代码没有。目前是纯 Token 数切分（`TokenTextSplitter`），优点是实现简单、中英混排友好；缺点是没有考虑句子边界、段落语义。进阶方案是引入**语义切分**（embedding 变化点检测），但会增加实现复杂度。

---

#### 6.2 检索策略是什么？是纯向量检索、关键词检索、还是混合检索？

**代码依据：**

```java
// KnowledgeBaseService.java - 第 43-45 行
QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.6)
                .build())
        .build()
```

```java
// KnowledgeBaseController.java - 第 63-69 行
vectorStore.similaritySearch(
        SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.6)
                .build()
);
```

**回答要点：**

当前代码使用的是**纯向量检索（Pure Dense Retrieval）**：

- 使用 DashScope 的 embedding 模型（阿里巴巴通义文本嵌入）将文档和查询都转为向量
- 检索时计算余弦相似度（cosine similarity），返回相似度 ≥ 0.6 的 topK 结果
- 默认 `topK = 5`，最多同时召回 5 个文档块

> **追问：为什么没有用 BM25 或混合检索？**

| 方案 | 优点 | 缺点 | 适用场景 |
|---|---|---|---|
| 纯 Dense | 语义理解强，能处理同义词、泛化查询 | 对专有名词/精确术语召回差，计算量大 | 语义模糊、概念性查询 |
| 纯 BM25 | 精确匹配快，不依赖模型 | 无法处理语义相关性 | 专业术语查询 |
| 混合检索（Hybrid） | 兼顾两者，召回率最高 | 实现复杂，需融合排序 | 生产环境推荐 |

当前项目如果对专业术语（如"燥郁症"）召回要求高，建议升级为**混合检索**——在 VectorStore 层面同时做向量相似度和 BM25 打分，用 RRF（Reciprocal Rank Fusion）融合排序。

---

#### 6.3 每次问答会从知识库中召回多少个文档块？召回太多会不会 Context 溢出？

**代码依据：**

```java
// KnowledgeBaseService.java - 第 44 行
.searchRequest(SearchRequest.builder().topK(5).similarityThreshold(0.6).build())
```

```java
// ChunkingDemoService.java - 第 16 行
512  // 每块最大 512 tokens
```

**回答要点：**

- **每次召回 5 个 chunk，每个 chunk 最多 512 tokens**
- 最大上下文量 = 5 × 512 = 2560 tokens，仅用于检索上下文
- 加上 system prompt（约 200 tokens）+ 用户问题 + 模型回复，一般远低于 32K context 上限

**Context 溢出风险评估：**

| 风险点 | 当前状态 | 评估 |
|---|---|---|
| 召回 chunk 数量 | topK=5 | ✅ 安全 |
| 单 chunk 大小 | 512 tokens | ✅ 安全 |
| 历史对话累积 | InMemoryChatMemory（多轮场景） | ⚠️ 潜在风险 |
| 模型 context 上限 | DashScope qwen 系列 32K+ | ✅ 安全 |

> **追问：InMemoryChatMemory 有什么隐患？**

```java
// KnowledgeBaseService.java - 第 46-50 行
MessageChatMemoryAdvisor.builder(
        MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build()
)
```

`InMemoryChatMemory` 是会话级别的历史存储，**不会自动清理**。如果用户长时间多轮对话，内存会持续增长。生产环境应该：
1. 使用 `MessageWindowChatMemory` 限制消息窗口大小（当前已在用，但未设置窗口大小）
2. 或者将 ChatMemory 持久化到 Redis/MySQL，避免重启丢失

---

### 7. 改写增强（Query Enhancement）

#### 7.1 这个问题改写属于 Query Rewrite 技术，除此之外还有哪些方法？各自的优缺点是什么？

**代码依据：**

```java
// EnhancedQAController.java - 第 17-23 行
@GetMapping("/ask-enhanced")
public String askEnhanced(@RequestParam String question) {
    // 先用模型改写成更规范的检索语句
    String rewrittenQuestion = knowledgeBaseService.askOnce(
            "将以下问题改写成更规范、适合文档检索的表述，只输出改写后的问题，不要解释：" + question);

    // 用改写后的问题做 RAG 问答
    return knowledgeBaseService.askOnce(rewrittenQuestion);
}
```

**回答要点：**

当前使用的是 **Query Rewrite（查询改写）**，主要目的是把口语化、模糊的问题转化为更适合检索的标准表述。

**其他 Query Enhancement 方法对比：**

| 方法 | 核心思想 | 优点 | 缺点 | 代码对应 |
|---|---|---|---|---|
| **Query Rewrite** | LLM 改写问题表述 | 效果好，泛化能力强 | 多一次 LLM 调用，延迟增加 | `EnhancedQAController` |
| **Query Decomposition** | 把复杂问题拆成多个子问题，分别检索后合并 | 适合多跳复杂问题 | 子问题数量不确定，合并策略复杂 | 无 |
| **HyDE（Hypothetical Document Embeddings）** | 让 LLM 先生成"假设答案"，用假设答案去检索真实文档 | 对模糊查询效果好 | 可能生成幻觉答案，导致检索偏离 | 无 |
| **Step-back Prompting** | 先抽象出高层概念/原则，再检索，再回答 | 避免过度关注细节 | 需要两次 prompt 设计 | 无 |
| **Query Expansion / 子查询生成** | 基于原问题生成多个相关子查询 | 扩大召回范围 | 召回噪声增加 | 无 |

> **追问：如何选择？**

- 简单单跳问题 → Query Rewrite 即可
- 复杂多跳问题 → Query Decomposition
- 模糊抽象问题 → HyDE + Rewrite
- 生产环境推荐 **混合策略**：Query Rewrite + 子查询扩展 + 混合检索

---

#### 7.2 如果用户的问题包含错误信息（比如把"抑郁症"错写成"燥郁症"），改写环节能否纠正这个错误？

**代码依据：**

```java
// EnhancedQAController.java - 第 19-20 行
String rewrittenQuestion = knowledgeBaseService.askOnce(
        "将以下问题改写成更规范、适合文档检索的表述，只输出改写后的问题，不要解释：" + question);
```

**回答要点：**

**理论上可以，实际上取决于 LLM 的能力边界：**

| 场景 | 能否纠正 | 说明 |
|---|---|---|
| 常见别字/词（"燥郁症"→"抑郁症"） | ✅ 大概率能 | LLM 有医学常识，能识别常见错误 |
| 专业术语混淆（"胰蛋白酶"→"胰淀粉酶"） | ⚠️ 看 LLM 知识储备 | 可能纠正，也可能将错就错 |
| 罕见错误（自造词、方言） | ❌ 大概率不能 | LLM 无法凭空纠正不存在的东西 |

**当前 prompt 的问题：**

```java
"将以下问题改写成更规范、适合文档检索的表述，只输出改写后的问题，不要解释："
```

这个 prompt **没有明确要求 LLM 纠正错误**，只是"改写成规范表述"。LLM 可能把"燥郁症"改成"燥郁症（抑郁症）"而不是直接纠正为"抑郁症"。

**改进建议：**

```java
// 改进后的 prompt
String prompt = """
    请将以下用户问题改写成适合文档检索的标准问题。
    规则：
    1. 如果问题中有事实性错误（如病名、人名、术语错误），请直接修正
    2. 只输出改写后的问题，不要任何解释或前缀
    问题：""" + question;
```

> **追问：有没有不需要 LLM 的纠错方法？**
>
> 可以建立**领域知识词典**（病名、术语对照表），在检索前用词典做规则替换。"燥郁症" → "双相情感障碍" 或 "抑郁症"。优点是不依赖 LLM，响应快；缺点是词典维护成本高。

---

## 附录：代码架构速查

```
FullRagKnowledgeBaseController.java
├── /upload        → ingestDocument() → PDF 解析 → TokenTextSplitter 切片 → 向量库
├── /chat          → ask()            → RAG + ChatMemory 多轮对话
├── /ask           → askOnce()        → 单次 RAG
└── /search        → search()         → 仅检索，返回 topK 结果

EnhancedQAController.java
└── /ask-enhanced  → askOnce(改写) → askOnce(RAG)  两步串行

检索配置（KnowledgeBaseService.java）
├── topK = 5
├── similarityThreshold = 0.6
├── embedding: DashScope text-embedding
└── 切块策略: 512 tokens max, 100 overlap, 5 min
```
