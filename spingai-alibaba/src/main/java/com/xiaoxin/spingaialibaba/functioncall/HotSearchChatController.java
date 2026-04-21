package com.xiaoxin.spingaialibaba.functioncall;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotsearch")
public class HotSearchChatController {

    private final ChatClient chatClient;
    private final HotSearchTools hotSearchTools;

    public HotSearchChatController(ChatClient.Builder builder, HotSearchTools hotSearchTools) {
        this.hotSearchTools = hotSearchTools;
        this.chatClient = builder
                .defaultSystem("你是一个个人小助手，如果我询问的问题是tool工具具有的功能，优先调用，不要胡编乱造。")
                .build();
    }   

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .tools(hotSearchTools)
                .call()
                .content();
    }
}
