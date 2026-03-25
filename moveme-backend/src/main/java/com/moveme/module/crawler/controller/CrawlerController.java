package com.moveme.module.crawler.controller;

import com.moveme.common.result.Result;
import com.moveme.module.crawler.entity.CrawlLog;
import com.moveme.module.crawler.service.CrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 爬虫管理接口（仅管理员可用）
 */
@Tag(name = "爬虫管理", description = "豆瓣电影数据爬取（仅管理员）")
@RestController
@RequestMapping("/api/v1/admin/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerService crawlerService;

    @Operation(summary = "按标签爬取电影")
    @PostMapping("/trigger")
    public Result<CrawlLog> triggerCrawl(
            @RequestParam(defaultValue = "热门") String tag,
            @RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 500) {
            return Result.error(400, "limit 范围 1~500");
        }
        CrawlLog crawlLog = crawlerService.crawlByTag(tag, limit);
        return Result.success(crawlLog);
    }

    @Operation(summary = "爬取豆瓣 Top250")
    @PostMapping("/trigger/top250")
    public Result<CrawlLog> triggerTop250() {
        CrawlLog crawlLog = crawlerService.crawlTop250();
        return Result.success(crawlLog);
    }

    @Operation(summary = "爬取单部电影详情")
    @PostMapping("/trigger/detail/{doubanId}")
    public Result<Void> triggerDetail(@PathVariable String doubanId) {
        crawlerService.crawlMovieDetail(doubanId);
        return Result.success(null);
    }

    @Operation(summary = "获取爬虫日志")
    @GetMapping("/logs")
    public Result<List<CrawlLog>> getCrawlLogs(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(crawlerService.getCrawlLogs(limit));
    }
}
