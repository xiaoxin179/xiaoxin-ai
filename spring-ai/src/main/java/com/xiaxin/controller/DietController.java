package com.xiaxin.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/diet")
public class DietController {

    record NutrientInfo(String name, String current, String recommended, String gap) {}

    record DietAnalysis(
            String overallScore,
            String summary,
            List<NutrientInfo> nutrients,
            List<String> problems,
            List<String> suggestions,
            List<String> recommendedFoods,
            String warning,
            String point
    ) {}

    record DietRequest(String description) {}

    private final ChatClient chatClient;

    public DietController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/analyze")
    public DietAnalysis analyze(@RequestBody DietRequest request) {
        return chatClient.prompt()
                .system("""
                    你是一个专业营养师，根据用户描述的饮食习惯进行详细分析。
                    """)
                .user("请分析以下饮食习惯：\n" + request.description())
                .call()
                .entity(DietAnalysis.class);
    }
}
