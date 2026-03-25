package com.moveme.module.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.crawler.entity.CrawlLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrawlLogMapper extends BaseMapper<CrawlLog> {
}
