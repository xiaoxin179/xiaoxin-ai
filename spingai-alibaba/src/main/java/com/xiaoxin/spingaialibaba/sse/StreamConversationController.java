package com.xiaoxin.spingaialibaba.sse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/stream/conversation")
public class StreamConversationController {

    private final ChatClient chatClient;
    // 单独持有 chatMemory，每次请求按 conversationId 构建 Advisor
    private final MessageWindowChatMemory chatMemory;

    public StreamConversationController(ChatClient.Builder builder) {
        //没有为MessageWindowChatMemory注入Repository，所以每次请求都会创建新的实例，导致无法持久化对话历史
        //这里直接 new 了一个内存版的 MessageWindowChatMemory，没有传入任何 Repository，意味着它用的是默认的内存存储。
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
        this.chatClient = builder
                .defaultSystem("你是一个 Java 技术助手")

                .build();
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId) {

        return chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .stream()
                .content()
                // 超时控制：30 秒内没有新数据就触发 TimeoutException
                .timeout(Duration.ofSeconds(1))
                // 超时时推送提示后结束流
                .onErrorResume(TimeoutException.class,
                        e -> Flux.just("[响应超时，请重试]"))
                // 其他异常统一处理
                .onErrorResume(e -> {
                    System.out.println("流失出错："+e.getMessage());
                    return Flux.just("[抱歉，生成过程中出现错误，请稍后重试]");
                })
                .doOnNext(chunk -> System.out.print(chunk))      // 每片到来时打印
                .doOnComplete(() -> System.out.println("\n完成")); // 全部完成时
    }
}