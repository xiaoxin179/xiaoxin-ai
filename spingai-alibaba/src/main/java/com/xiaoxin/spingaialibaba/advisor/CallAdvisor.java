package com.xiaoxin.spingaialibaba.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

public interface CallAdvisor extends Advisor {
    // 核心方法：包裹整个调用过程
    ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain);
    // 优先级，数字越小越先执行
    int getOrder();
}