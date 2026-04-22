package com.xiaoxin.spingaialibaba.advisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logging-advisor")
@Slf4j
public class LoggingAdvisorController {

    private final ChatClient chatClient;
    private final LoggingAdvisor loggingAdvisor;
    private final ChatMemory chatMemory;

    public LoggingAdvisorController(ChatClient.Builder builder, LoggingAdvisor loggingAdvisor, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.loggingAdvisor = loggingAdvisor;
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")
                .defaultAdvisors(
                        loggingAdvisor
                )
                .build();
    }

    @GetMapping
    public String chat(@RequestParam String message, @RequestParam String conversationId) {
        try {
            String result = chatClient.prompt()
                    .user(message)
                    .advisors(
                            MessageChatMemoryAdvisor.builder(chatMemory).conversationId(conversationId).build(),
                            loggingAdvisor)
                    .call()
                    .content();
            log.info("========== 接口调用成功 ==========");
            return result;
        } catch (Exception e) {
            log.error("========== 接口调用出错 ==========", e);
            return "出错了: " + e.getMessage();
        }
    }
}