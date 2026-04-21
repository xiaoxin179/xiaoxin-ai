package com.xiaoxin.spingaialibaba.crawler;

import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaiduHotSearchCrawlerTest {

  public static void test1() {
      BaiduHotSearchCrawler baiduHotSearchCrawler = new BaiduHotSearchCrawler();
      List<String> strings = baiduHotSearchCrawler.fetchHotSearch();
      System.out.println(strings);
  }

    public static void main(String[] args) {
        test1();
    }

}