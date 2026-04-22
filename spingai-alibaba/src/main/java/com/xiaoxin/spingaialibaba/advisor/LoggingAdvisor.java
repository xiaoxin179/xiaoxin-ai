package com.xiaoxin.spingaialibaba.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);

    public LoggingAdvisor() {
        log.info("========== LoggingAdvisor 构造方法执行，已注册 ==========");
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("========== LoggingAdvisor.adviseCall 被调用了 ==========");
        log.info("请求内容: {}", request.prompt().getContents());
        long start = System.currentTimeMillis();

        ChatClientResponse response;
        try {
            response = chain.nextCall(request);
        } catch (Exception e) {
            log.error("========== chain.nextCall 执行出错 ==========", e);
            throw e;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[AI调用] 耗时: {}ms", elapsed);
        if (response != null && response.chatResponse() != null) {
            String aiReply = response.chatResponse().getResult().getOutput().getText();
            log.info("[AI调用] 模型回复: {}", aiReply);
        } else {
            log.warn("[AI调用] 响应为空");
        }

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        log.info("========== LoggingAdvisor.adviseStream 被调用了 ==========");
        long start = System.currentTimeMillis();
        log.info("流式请求内容: {}", request.prompt().getContents());

        return chain.nextStream(request)
                .doOnComplete(() -> log.info("[AI流式调用] 完成，耗时 {}ms",
                        System.currentTimeMillis() - start));
    }

    @Override
    public String getName() {
        return "CUSLoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}