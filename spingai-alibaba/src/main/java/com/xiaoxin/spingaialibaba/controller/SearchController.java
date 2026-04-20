package com.xiaoxin.spingaialibaba.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/qwen/search")
public class SearchController {

    private final ChatClient chatClient;

    public SearchController(DashScopeChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel).build();
    }

    @GetMapping
    public String search(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen-max")
                        .withEnableSearch(true)
                        .build())
                .call()
                .content();
    }

    @GetMapping("/think")
    public String think(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen3-235b-a22b")
                        .withEnableThinking(true)
                        .withThinkingBudget(2000)
                        .build())
                .call()
                .content();
    }

    @GetMapping(value = "/think/stream", produces = "text/event-stream")
    public Flux<String> thinkStream(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen3-235b-a22b")
                        .withEnableThinking(true)
                        .withThinkingBudget(2000)
                        .build())
                .stream()
                .content();
    }
}
