package com.moveme.module.crawler.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CrawlTriggerReq {

    /** 任务类型：SINGLE / TOP250 / CHART / ANNUAL / COMMENTS / EXTENDED_INFO / ENRICH */
    private String taskType;

    /** 可选参数：subjectId, year, limit, start_rank, input 等 */
    private Map<String, String> params = new HashMap<>();
}
