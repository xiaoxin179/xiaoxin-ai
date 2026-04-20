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
