package com.xiaoxin.spingaialibaba.controller;

import com.xiaoxin.spingaialibaba.config.ChatClientEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final ChatClient customerServiceChatClient;
    private final ChatClient contentChatClient;

    public BusinessController(
            @Qualifier(ChatClientEnum.CUSTOMER_SERVICE) ChatClient customerServiceChatClient,
            @Qualifier(ChatClientEnum.CONTENT) ChatClient contentChatClient) {
        this.customerServiceChatClient = customerServiceChatClient;
        this.contentChatClient = contentChatClient;
    }

    @GetMapping(value = "/customer-service/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> customerServiceStream(@RequestParam String message) {
        return customerServiceChatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    @GetMapping("/customer-service")
    public String customerService(@RequestParam String message) {
        return customerServiceChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping(value = "/generate-copy/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateCopyStream(@RequestParam String brief) {
        return contentChatClient.prompt()
                .user("根据以下简报，生成一段产品推广文案,按照雷军吹牛逼的风格：\n" + brief)
                .stream()
                .content();
    }

    @GetMapping("/generate-copy")
    public String generateCopy(@RequestParam String brief) {
        return contentChatClient.prompt()
                .user("根据以下简报，生成一段产品推广文案,按照雷军吹牛逼的风格：\n" + brief)
                .call()
                .content();
    }
}
