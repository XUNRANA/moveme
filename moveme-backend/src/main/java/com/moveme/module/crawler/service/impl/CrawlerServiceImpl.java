package com.moveme.module.crawler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.common.exception.BusinessException;
import com.moveme.common.result.ResultCode;
import com.moveme.module.crawler.config.CrawlerProperties;
import com.moveme.module.crawler.entity.CrawlLog;
import com.moveme.module.crawler.mapper.CrawlLogMapper;
import com.moveme.module.crawler.service.CrawlerService;
import com.moveme.module.crawler.service.CrawlerTaskType;
import com.moveme.module.crawler.vo.CrawlerStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {

    private final CrawlLogMapper crawlLogMapper;
    private final CrawlerProperties crawlerProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CrawlerAsyncHelper crawlerAsyncHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Long triggerCrawl(CrawlerTaskType taskType, Map<String, String> params) {
        if (!crawlerProperties.isEnabled()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "爬虫模块未启用");
        }
        if (isRunning(taskType)) {
            throw new BusinessException(ResultCode.CRAWLER_ALREADY_RUNNING,
                    "爬虫任务已在运行: " + taskType.getDisplayName());
        }

        CrawlLog logRow = createLog(taskType, params);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path outputPath = Paths.get(crawlerProperties.getDataDir(), taskType.name().toLowerCase() + "_" + ts + ".json");
        List<String> cmd = buildCommand(taskType, params, outputPath);
        crawlerAsyncHelper.runCrawlAsync(taskType, params, logRow, cmd, outputPath);
        return logRow.getId();
    }

    @Override
    public boolean isRunning(CrawlerTaskType taskType) {
        String lockKey = RedisKeyConstants.CRAWLER_LOCK_PREFIX + taskType.name().toLowerCase();
        Boolean exists = redisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public CrawlerStatusVO getStatus() {
        CrawlerStatusVO vo = new CrawlerStatusVO();

        Map<String, Boolean> running = new LinkedHashMap<>();
        for (CrawlerTaskType t : CrawlerTaskType.values()) {
            running.put(t.name(), isRunning(t));
        }
        vo.setRunningTasks(running);

        Page<CrawlLog> page = crawlLogMapper.selectPage(
                new Page<>(1, 10),
                new LambdaQueryWrapper<CrawlLog>()
                        .orderByDesc(CrawlLog::getStartedAt));
        vo.setRecentLogs(page.getRecords().stream()
                .map(this::toLogEntry)
                .toList());

        return vo;
    }

    // ==================== private helpers ====================

    private CrawlLog createLog(CrawlerTaskType taskType, Map<String, String> params) {
        CrawlLog row = new CrawlLog();
        row.setTaskType(taskType.name());
        row.setStatus(0); // 0 = RUNNING
        row.setStartedAt(LocalDateTime.now());
        try {
            row.setParamsJson(objectMapper.writeValueAsString(params));
        } catch (Exception ignored) {
            row.setParamsJson("{}");
        }
        crawlLogMapper.insert(row);
        return row;
    }

    List<String> buildCommand(CrawlerTaskType taskType, Map<String, String> params, Path outputPath) {
        CrawlerProperties.Python py = crawlerProperties.getPython();
        List<String> cmd = new ArrayList<>();
        cmd.add(py.getExecutable());
        cmd.add(py.getScriptsDir() + taskType.getScript());

        String defaultArgs = taskType.getDefaultArgs();
        if (defaultArgs != null && !defaultArgs.isEmpty()) {
            String resolved = resolveArgs(defaultArgs, params);
            if (!resolved.isEmpty()) {
                for (String arg : resolved.split(" ")) {
                    if (!arg.isEmpty()) cmd.add(arg);
                }
            }
        }

        // Extra params become --key value
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                String key = e.getKey();
                if ("subjectId".equals(key)) continue; // handled by defaultArgs
                cmd.add("--" + key.replace('_', '-'));
                if (StringUtils.hasText(e.getValue())) {
                    cmd.add(e.getValue());
                }
            }
        }

        cmd.add("-o");
        cmd.add(outputPath.toString());
        cmd.add("--db");

        if (StringUtils.hasText(py.getCookie())) {
            cmd.add("--cookie");
            cmd.add(py.getCookie());
        }
        cmd.add("--sleep");
        cmd.add(String.valueOf(py.getSleepSeconds()));
        cmd.add("--timeout");
        cmd.add(String.valueOf(py.getRequestTimeout()));

        return cmd;
    }

    private String resolveArgs(String template, Map<String, String> params) {
        if (params == null || params.isEmpty()) return template;
        String resolved = template;
        for (Map.Entry<String, String> e : params.entrySet()) {
            resolved = resolved.replace("{" + e.getKey() + "}", e.getValue());
        }
        // Remove unresolved placeholders
        resolved = resolved.replaceAll("\\{[^}]+\\}", "");
        return resolved.trim();
    }

    private CrawlerStatusVO.CrawlLogEntry toLogEntry(CrawlLog row) {
        CrawlerStatusVO.CrawlLogEntry entry = new CrawlerStatusVO.CrawlLogEntry();
        entry.setId(row.getId());
        entry.setTaskType(row.getTaskType());
        entry.setStatus(row.getStatus());
        entry.setStatusText(switch (row.getStatus()) {
            case 0 -> "运行中";
            case 1 -> "成功";
            case 2 -> "失败";
            default -> "未知";
        });
        entry.setTotalCount(row.getTotalCount());
        entry.setSuccessCount(row.getSuccessCount());
        entry.setFailCount(row.getFailCount());
        entry.setMoviesImported(row.getMoviesImported());
        entry.setErrorMessage(row.getErrorMessage());
        entry.setStartedAt(row.getStartedAt());
        entry.setFinishedAt(row.getFinishedAt());
        return entry;
    }
}
