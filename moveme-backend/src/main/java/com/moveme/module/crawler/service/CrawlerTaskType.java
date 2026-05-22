package com.moveme.module.crawler.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrawlerTaskType {

    /** 爬取单部电影详情：python douban_movie_spider.py {subjectId} */
    SINGLE("douban_movie_spider.py", "{subjectId}", "单部电影详情"),

    /** 爬取 Top250 榜单：python douban_movie_spider.py --top250 */
    TOP250("douban_movie_spider.py", "--top250", "Top250 榜单"),

    /** 爬取豆瓣新片榜、口碑榜、北美票房榜、分类排行榜 */
    CHART("crawl_movie_chart.py", "--include-type-ranks", "豆瓣榜单"),

    /** 爬取年度榜单：python crawl_movie_annual.py {year} */
    ANNUAL("crawl_movie_annual.py", "{year}", "年度榜单"),

    /** 爬取短评：python crawl_comments.py --input {input} */
    COMMENTS("crawl_comments.py", "--input {input}", "短评"),

    /** 扩展信息（播放链接、长评、台词） */
    EXTENDED_INFO("crawl_extended_info.py", "--input {input}", "扩展信息"),

    /** 推荐数据增强（评分分布、获奖、影人、相关电影） */
    ENRICH("enrich_recommendation_data.py", "--input {input}", "推荐数据增强");

    private final String script;
    private final String defaultArgs;
    private final String displayName;
}
