package com.moveme.module.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminStatsVO {
    private long movieCount;
    private long userCount;
    private long todayNewUsers;
    private long ratingCount;
    private String lastCrawlStatus;
    private LocalDateTime lastCrawlTime;
    private long recoLogCount;
    private long favoriteCount;
    private long viewHistoryCount;
    private long searchHistoryCount;
    private long personCount;
    private long genreCount;
    private long crawlLogCount;
    private long importLogCount;
    private long movieCommentCount;
}
