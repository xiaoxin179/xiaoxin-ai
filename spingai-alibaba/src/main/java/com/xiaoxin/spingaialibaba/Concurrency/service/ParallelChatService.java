package com.xiaoxin.spingaialibaba.Concurrency.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ParallelChatService {

    private static final Logger log = LoggerFactory.getLogger(ParallelChatService.class);

    private final ChatClient deepseekChatClient;
    private final ChatClient qwenChatClient;

    public ParallelChatService(
            @Qualifier("primaryChatClient") ChatClient deepseekChatClient,
            @Qualifier("backupChatClient") ChatClient qwenChatClient) {
        this.deepseekChatClient = deepseekChatClient;
        this.qwenChatClient = qwenChatClient;
    }

    /**
     * 并行调用两个模型，总耗时 ≈ max(A耗时, B耗时)
     */
    public Map<String, String> parallelChat(String question) throws Exception {
        long start = System.currentTimeMillis();

        // 两个请求同时发出，各自在独立线程里跑
        CompletableFuture<String> deepseekFuture = CompletableFuture.supplyAsync(
                () -> deepseekChatClient.prompt().user(question).call().content()
        );
        CompletableFuture<String> qwenFuture = CompletableFuture.supplyAsync(
                () -> qwenChatClient.prompt().user(question).call().content()
        );

        // allOf 等两个都完成，超时 30s 抛异常
        CompletableFuture.allOf(deepseekFuture, qwenFuture).get(30, TimeUnit.SECONDS);

        long cost = System.currentTimeMillis() - start;
        log.info("并行请求完成，总耗时: {} ms", cost);

        return Map.of(
                "deepseek", deepseekFuture.get(),
                "qwen", qwenFuture.get()
        );
    }
}