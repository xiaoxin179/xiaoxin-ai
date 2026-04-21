package com.xiaoxin.spingaialibaba.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/book")
public class BookController {

    record BookSummary(String title, String author, String oneLinerSummary) {}

    private final ChatClient chatClient;

    public BookController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    //避免再运行的时候范型被擦除掉，匿名内部类的方式，在类加载时捕获完整的泛型类型签名，Spring AI / RestTemplate / WebClient 在反序列化 JSON 时，就能知道要转成 List<BookSummary>
    @GetMapping("/list")
    public List<BookSummary> list() {
        return chatClient.prompt()
                .user("列出 5 本经典的 Java 技术书籍")
                .call()
                .entity(new ParameterizedTypeReference<List<BookSummary>>() {});
    }
}