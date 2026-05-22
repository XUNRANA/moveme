package com.moveme.module.crawler.scheduler;

import com.moveme.module.crawler.config.CrawlerProperties;
import com.moveme.module.crawler.service.CrawlerService;
import com.moveme.module.crawler.service.CrawlerTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "moveme.crawler.enabled", havingValue = "true", matchIfMissing = true)
public class CrawlerScheduler {

    private final CrawlerService crawlerService;
    private final CrawlerProperties crawlerProperties;

    /** 每周一凌晨 3:00 爬取豆瓣榜单（新片榜、口碑榜、分类榜） */
    @Scheduled(cron = "${moveme.crawler.schedule.chart-cron}")
    public void weeklyChartCrawl() {
        log.info("Scheduled chart crawl triggered");
        try {
            crawlerService.triggerCrawl(CrawlerTaskType.CHART, Map.of("include_type_ranks", "true"));
        } catch (Exception e) {
            log.error("Scheduled chart crawl failed", e);
        }
    }

    /** 每周一凌晨 4:00 爬取短评 */
    @Scheduled(cron = "${moveme.crawler.schedule.comments-cron}")
    public void weeklyCommentsCrawl() {
        log.info("Scheduled comments crawl triggered");
        try {
            crawlerService.triggerCrawl(CrawlerTaskType.COMMENTS, Map.of());
        } catch (Exception e) {
            log.error("Scheduled comments crawl failed", e);
        }
    }

    /** 每年 1 月 1 日凌晨 3:00 爬取年度榜单 */
    @Scheduled(cron = "${moveme.crawler.schedule.annual-cron}")
    public void annualCrawl() {
        String year = String.valueOf(Year.now().getValue());
        log.info("Scheduled annual crawl triggered: year={}", year);
        try {
            crawlerService.triggerCrawl(CrawlerTaskType.ANNUAL, Map.of("year", year));
        } catch (Exception e) {
            log.error("Scheduled annual crawl failed", e);
        }
    }

    /** 每周一凌晨 5:00 数据增强（评分分布、获奖、影人详情） */
    @Scheduled(cron = "${moveme.crawler.schedule.enrich-cron}")
    public void weeklyEnrichCrawl() {
        log.info("Scheduled enrich crawl triggered");
        try {
            crawlerService.triggerCrawl(CrawlerTaskType.ENRICH, Map.of("force", "true"));
        } catch (Exception e) {
            log.error("Scheduled enrich crawl failed", e);
        }
    }
}
