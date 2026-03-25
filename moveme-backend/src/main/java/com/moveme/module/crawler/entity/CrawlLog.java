package com.moveme.module.crawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crawl_logs")
public class CrawlLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskType;
    private Integer status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
