package com.xiaoxin.spingaialibaba.rag.rag_search.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qa")
public class KnowledgeQAController {

    private final ChatClient ragChatClient;

    public KnowledgeQAController(@Qualifier("ragChatClient") ChatClient ragChatClient) {
        this.ragChatClient = ragChatClient;
    }

    @GetMapping
    public String ask(@RequestParam String question) {
        return ragChatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}