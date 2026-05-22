package com.moveme.module.crawler.controller;

import com.moveme.common.result.Result;
import com.moveme.module.crawler.dto.CrawlTriggerReq;
import com.moveme.module.crawler.service.CrawlerService;
import com.moveme.module.crawler.service.CrawlerTaskType;
import com.moveme.module.crawler.vo.CrawlerStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "爬虫管理", description = "豆瓣爬虫任务触发与状态查询")
@RestController
@RequestMapping("/api/v1/admin/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerService crawlerService;

    @Operation(summary = "触发爬虫任务")
    @PostMapping("/trigger")
    public Result<Long> trigger(@RequestBody CrawlTriggerReq req) {
        CrawlerTaskType taskType = CrawlerTaskType.valueOf(req.getTaskType().toUpperCase());
        Map<String, String> params = req.getParams() != null ? req.getParams() : Map.of();
        Long logId = crawlerService.triggerCrawl(taskType, params);
        return Result.success(logId);
    }

    @Operation(summary = "查看爬虫状态")
    @GetMapping("/status")
    public Result<CrawlerStatusVO> status() {
        return Result.success(crawlerService.getStatus());
    }

    @Operation(summary = "爬取单部电影")
    @PostMapping("/movie/{subjectId}")
    public Result<Long> crawlMovie(@PathVariable String subjectId) {
        Long logId = crawlerService.triggerCrawl(CrawlerTaskType.SINGLE,
                Map.of("subjectId", subjectId));
        return Result.success(logId);
    }
}
