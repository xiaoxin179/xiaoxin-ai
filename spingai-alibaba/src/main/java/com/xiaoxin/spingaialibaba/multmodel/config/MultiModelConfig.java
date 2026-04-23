package com.xiaoxin.spingaialibaba.multmodel.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MultiModelConfig {

    /**
     * 主力 ChatClient：DeepSeek，便宜，适合高频调用
     */
    @Bean("primaryChatClient")
    @Primary
    public ChatClient primaryChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个专业的助手")
                .build();
    }

    /**
     * 主力 Builder：供原有代码注入使用（原来没有多 ChatClient 时直接注入 Builder）
     */
    @Bean
    @Primary
    public ChatClient.Builder primaryChatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel);
    }

    /**
     * 主力 EmbeddingModel：解决 PgVectorStore 注入了两个 EmbeddingModel 的冲突
     */
    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(OpenAiEmbeddingModel openAiEmbeddingModel) {
        return openAiEmbeddingModel;
    }

    /**
     * 备用 ChatClient：通义千问，主力挂了时使用
     */
    @Bean("backupChatClient")
    public ChatClient backupChatClient(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一个专业的助手")
                .build();
    }

    /**
     * 备用 Builder：供 @Qualifier("backupChatClient") ChatClient.Builder 的注入使用
     */
    @Bean("backupChatClientBuilder")
    public ChatClient.Builder backupChatClientBuilder(DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel);
    }
}