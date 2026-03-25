package com.moveme.module.crawler.service;

import com.moveme.module.crawler.entity.CrawlLog;

import java.util.List;

public interface CrawlerService {

    /**
     * 按豆瓣标签爬取电影列表（热门、最新、经典等）
     * @param tag   标签名
     * @param limit 最大爬取数量
     * @return 爬虫日志
     */
    CrawlLog crawlByTag(String tag, int limit);

    /**
     * 爬取豆瓣 Top 250
     */
    CrawlLog crawlTop250();

    /**
     * 爬取单部电影详情（导演、演员、简介等）
     * @param doubanId 豆瓣 ID
     */
    void crawlMovieDetail(String doubanId);

    /**
     * 获取爬虫日志列表
     */
    List<CrawlLog> getCrawlLogs(int limit);
}
