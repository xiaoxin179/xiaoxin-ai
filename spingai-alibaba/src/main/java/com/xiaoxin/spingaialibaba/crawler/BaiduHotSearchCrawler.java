package com.xiaoxin.spingaialibaba.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BaiduHotSearchCrawler {

    private static final String BAIDU_URL = "https://www.baidu.com";    

    /**
     * 获取百度热搜列表
     * @return 热搜标题列表
     */
    public List<String> fetchHotSearch() {
        List<String> hotList = new ArrayList<>();
        try {
            String html = Jsoup.connect(BAIDU_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .ignoreHttpErrors(true)
                    .execute()
                    .body();
            Document doc = Jsoup.parse(html);

            // 百度热搜通常在 #hotsearch-content 或者 .s-hotsearch-content 中
            Element hotContent = doc.selectFirst("#hotsearch-content, .s-hotsearch-content, #s_hotsearch_content");
            if (hotContent != null) {
                Elements items = hotContent.select("a");
                for (Element item : items) {
                    String title = item.text().trim();
                    if (!title.isEmpty()) {
                        hotList.add(title);
                    }
                }
            }

            // 备选：直接从页面中提取包含"热搜"关键字的链接文本
            if (hotList.isEmpty()) {
                Elements allLinks = doc.select("a[href*='https://top.baidu.com']");
                for (Element link : allLinks) {
                    String title = link.text().trim();
                    if (!title.isEmpty() && title.length() > 2) {
                        hotList.add(title);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("爬取百度热搜失败: " + e.getMessage());
        }
        return hotList;
    }

    /**
     * 获取带排名的热搜列表
     * @return 格式为 "1. 热搜标题" 的列表
     */
    public List<String> fetchHotSearchWithRank() {
        List<String> result = new ArrayList<>();
        List<String> hotList = fetchHotSearch();
        for (int i = 0; i < hotList.size(); i++) {
            result.add((i + 1) + ". " + hotList.get(i));
        }
        return result;
    }

    /**
     * 获取格式化后的热搜字符串
     * @return 换行分隔的热搜内容
     */
    public String fetchHotSearchAsString() {
        List<String> rankedList = fetchHotSearchWithRank();
        return String.join("\n", rankedList);
    }
}
