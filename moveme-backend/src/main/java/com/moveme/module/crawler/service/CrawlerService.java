package com.moveme.module.crawler.service;

import com.moveme.module.crawler.vo.CrawlerStatusVO;

import java.util.Map;

public interface CrawlerService {

    /** 触发一次爬虫任务，返回 crawl_logs.id */
    Long triggerCrawl(CrawlerTaskType taskType, Map<String, String> params);

    /** 检查指定类型的爬虫是否正在运行 */
    boolean isRunning(CrawlerTaskType taskType);

    /** 获取所有爬虫的运行状态 + 最近日志 */
    CrawlerStatusVO getStatus();
}
