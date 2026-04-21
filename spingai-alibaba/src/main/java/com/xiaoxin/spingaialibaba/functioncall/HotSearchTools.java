package com.xiaoxin.spingaialibaba.functioncall;

import com.xiaoxin.spingaialibaba.crawler.BaiduHotSearchCrawler;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HotSearchTools {

    private final BaiduHotSearchCrawler crawler;

    public HotSearchTools(BaiduHotSearchCrawler crawler) {
        this.crawler = crawler;
    }

    /**
     * 获取当前百度热搜排行榜
     * @return 带排名的热搜列表，每行格式为 "1. 热搜标题"
     */
    public String getBaiduHotSearch() {
        return crawler.fetchHotSearchAsString();
    }

    /**
     * 获取当前百度热搜（简洁列表）
     * @return 热搜标题列表
     */
    @Tool(description = "获取百度上的热搜列表")
    public List<String> getBaiduHotSearchList() {
        System.out.println("函数被大模型调用-------");
        return crawler.fetchHotSearch();
    }
}
