package com.moveme.module.crawler.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CrawlerStatusVO {

    private Map<String, Boolean> runningTasks;
    private List<CrawlLogEntry> recentLogs;

    @Data
    public static class CrawlLogEntry {
        private Long id;
        private String taskType;
        private Integer status;
        private String statusText;
        private Integer totalCount;
        private Integer successCount;
        private Integer failCount;
        private Integer moviesImported;
        private String errorMessage;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }
}
