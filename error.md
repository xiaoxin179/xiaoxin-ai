# OpenAI 与通义千问自动配置冲突问题

## 问题描述

启动 `spingai-alibaba` 时报错，提示 `OpenAI API key must be set`：

```
Error creating bean with name 'openAiAudioSpeechModel'
Error creating bean with name 'openAiAudioTranscriptionModel'
Error creating bean with name 'openAiChatModel'
...
```

明明只用通义千问，却报 OpenAI 缺 API key。

## 根本原因

Maven 多模块项目中，父 pom 的 `<dependencies>` 会被所有子模块自动继承。

父 pom 中引入了：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

这导致**所有子模块**都带入了 OpenAI 的自动配置类（`OpenAiChatAutoConfiguration`、`OpenAiEmbeddingAutoConfiguration`、`OpenAiAudioSpeechAutoConfiguration`、`OpenAiAudioTranscriptionAutoConfiguration` 等）。

而 `spingai-alibaba` 子模块又同时引入了 `spring-ai-alibaba-starter-dashscope`，两套自动配置同时生效，Spring Boot 在启动时尝试创建 OpenAI 的各种 Bean，但找不到 API key，导致启动失败。

## 解决方案

将 `spring-ai-starter-model-openai` 从父 pom 的 `<dependencies>` 中移除，改为只在使用它的模块中单独引入。

父 pom 修改：

```xml
<!-- 删除此依赖 -->
<!--
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
-->

<!-- 保留 test 依赖即可 -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

`spring-ai-alibaba` 子模块的 `spring-ai-autoconfigure-model-openai` 排除配置也可以一并移除，因为父 pom 不再带入 OpenAI starter。

## 经验总结

多模块 Maven 项目中，非通用依赖（如特定 AI 模型的 starter）应尽量放在具体子模块的 pom 中，而非父 pom 的 `<dependencies>`，避免影响不需要它的其他子模块。
# 添加一个advisor之后实际上日志没有被打印出来？

## 问题描述

实现了 `CallAdvisor` 接口的 `LoggingAdvisor` 已注册为 Spring Bean，构造方法正常打印了日志，但调用接口后 `adviseCall` 方法从未被触发，控制台没有任何输出。

## 根本原因

有两个问题叠加：

### 1. 自定义接口文件覆盖了 Spring AI 原生接口

项目中存在一个自定义的 `advisor/CallAdvisor.java` 文件：

```java
public interface CallAdvisor extends Advisor {
    ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain);
    int getOrder();
}
```

这个自定义接口只有两个方法，而 Spring AI 原生的 `org.springframework.ai.chat.client.advisor.api.CallAdvisor` 还继承了 `Advisor`（要求 `getName()` 方法）。由于自定义文件先被编译，它干扰了 `LoggingAdvisor` 的接口实现，导致 advisor 链无法正确识别和调用。

### 2. `.advisors()` 与 `.defaultAdvisors()` 行为不一致

将 `LoggingAdvisor` 通过 `.advisors(loggingAdvisor)` 传入时，没有配合 Spring AI 内置的 `SimpleLoggerAdvisor` 验证机制是否正常。而 `AdvisorDemoController` 中的 `SimpleLoggerAdvisor` 是通过 `.defaultAdvisors()` 注册的。

## 解决方案

1. **删除自定义的 `CallAdvisor.java` 接口文件**，使用 Spring AI 原生的完整接口：

```java
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;

@Component
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("请求内容: {}", request.prompt().getContents());
        ChatClientResponse response = chain.nextCall(request);
        log.info("模型回复: {}", response.chatResponse().getResult().getOutput().getText());
        return response;
    }

    @Override
    public String getName() { return "CUSLoggingAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ...
    }
}
```

2. **通过 `defaultAdvisors` 注册**，而非在每次请求时通过 `.advisors()` 传入：

```java
public LoggingAdvisorController(ChatClient.Builder builder, LoggingAdvisor loggingAdvisor, ChatMemory chatMemory) {
    this.chatClient = builder
            .defaultSystem("你是一个 Java 技术助手")
            .defaultAdvisors(
                    loggingAdvisor,
                    MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
}
```

## 经验总结

- Spring Boot 项目中避免自定义与第三方库同名的接口/类，防止编译顺序导致意外覆盖
- 调试 advisor 链时，先用 Spring AI 内置的 `SimpleLoggerAdvisor` 验证机制是否正常工作，再排查自定义 advisor 的问题
- `defaultAdvisors` 是推荐的企业级用法，将通用的 advisor（内存、日志等）在构建 `ChatClient` 时一次性配置好，而不是在每次请求时重复传入
