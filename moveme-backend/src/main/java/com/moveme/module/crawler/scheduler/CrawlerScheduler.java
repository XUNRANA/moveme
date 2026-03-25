package com.moveme.module.crawler.scheduler;

import com.moveme.module.crawler.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 爬虫定时任务
 * - 每天凌晨 3:00 爬取热门 + 最新电影
 * - 每周日凌晨 4:00 刷新 Top 250
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final CrawlerService crawlerService;

    @Value("${moveme.crawler.enabled:false}")
    private boolean crawlerEnabled;

    /**
     * 每天凌晨 3:00 — 爬取热门电影（50部）+ 最新电影（50部）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyCrawl() {
        if (!crawlerEnabled) {
            log.debug("爬虫定时任务已关闭，跳过每日爬取");
            return;
        }
        log.info("===== 每日爬虫任务开始 =====");
        try {
            crawlerService.crawlByTag("热门", 50);
            crawlerService.crawlByTag("最新", 50);
        } catch (Exception e) {
            log.error("每日爬虫任务异常", e);
        }
        log.info("===== 每日爬虫任务结束 =====");
    }

    /**
     * 每周日凌晨 4:00 — 刷新豆瓣高分 Top 250
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void weeklyTop250() {
        if (!crawlerEnabled) {
            log.debug("爬虫定时任务已关闭，跳过 Top250 爬取");
            return;
        }
        log.info("===== 每周 Top250 爬虫任务开始 =====");
        try {
            crawlerService.crawlTop250();
        } catch (Exception e) {
            log.error("Top250 爬虫任务异常", e);
        }
        log.info("===== 每周 Top250 爬虫任务结束 =====");
    }
}
