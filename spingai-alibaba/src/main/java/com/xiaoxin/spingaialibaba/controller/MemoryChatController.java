package com.xiaoxin.spingaialibaba.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/memory-chat")
public class MemoryChatController {

    private final ChatClient chatClient;
    // 单独持有 chatMemory 实例，以便在每次请求时按 conversationId 构建 Advisor
    private final MessageWindowChatMemory chatMemory;

    public MemoryChatController(ChatClient.Builder builder) {
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .build();
    }

    /**
     * 多轮对话接口
     * conversationId 用来区分不同的会话
     */
    @GetMapping
    public String chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId) {
        List<Message> history = chatMemory.get(conversationId);

        System.out.println("===== 调用之前的 chatMemory =====");
        System.out.println("conversationId: " + conversationId);
        System.out.println("历史消息数量: " + (history == null ? 0 : history.size()));
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                Message msg = history.get(i);
                System.out.println("  [" + i + "] " + msg.getClass().getSimpleName()
                        + " | " + msg.getMessageType()
                        + " | " + msg.getText());
            }
        }
        System.out.println("本次用户输入: " + message);
        System.out.println("================================");

        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();
    }
}