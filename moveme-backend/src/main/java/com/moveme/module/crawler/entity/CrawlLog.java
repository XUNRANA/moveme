package com.moveme.module.crawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField(exist = false)
    private String paramsJson;

    @TableField(exist = false)
    private String pythonOutputFile;

    @TableField(exist = false)
    private Integer moviesImported;

    @TableField(exist = false)
    private String errors;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
