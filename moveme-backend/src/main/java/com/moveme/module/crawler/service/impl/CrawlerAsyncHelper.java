package com.moveme.module.crawler.service.impl;

import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.module.crawler.config.CrawlerProperties;
import com.moveme.module.crawler.entity.CrawlLog;
import com.moveme.module.crawler.mapper.CrawlLogMapper;
import com.moveme.module.crawler.service.CrawlerTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerAsyncHelper {

    private final CrawlLogMapper crawlLogMapper;
    private final CrawlerProperties crawlerProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    public void runCrawlAsync(CrawlerTaskType taskType, Map<String, String> params,
                              CrawlLog logRow, List<String> cmd, Path outputPath) {
        String lockKey = RedisKeyConstants.CRAWLER_LOCK_PREFIX + taskType.name().toLowerCase();
        int lockTimeout = crawlerProperties.getLockTimeoutMinutes();

        try {
            redisTemplate.opsForValue().set(lockKey, "1", lockTimeout, TimeUnit.MINUTES);
            Files.createDirectories(outputPath.getParent());

            log.info("Starting crawl: taskType={} cmd={}", taskType, cmd);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(crawlerProperties.getPython().getScriptsDir()));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String stdout = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(crawlerProperties.getPython().getRequestTimeout() * 60, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python process timed out");
            }

            int exitCode = process.exitValue();
            log.info("Python process exited: exitCode={} output={} chars", exitCode, stdout.length());

            if (exitCode != 0) {
                String tail = stdout.length() > 2000
                        ? stdout.substring(stdout.length() - 2000) : stdout;
                throw new RuntimeException("Python exit code " + exitCode + ": " + tail);
            }

            logRow.setStatus(1); // 1 = SUCCESS
            invalidateMovieCache();

        } catch (Exception e) {
            log.error("Crawl failed: taskType={}", taskType, e);
            logRow.setStatus(2); // 2 = FAILED
            logRow.setErrorMessage(e.getMessage());
        } finally {
            logRow.setFinishedAt(LocalDateTime.now());
            if (outputPath != null) {
                logRow.setPythonOutputFile(outputPath.toString());
            }
            crawlLogMapper.updateById(logRow);
            redisTemplate.delete(lockKey);
        }
    }

    private void invalidateMovieCache() {
        try {
            Set<String> detailKeys = redisTemplate.keys(RedisKeyConstants.MOVIE_DETAIL_PREFIX + "*");
            Set<String> pageKeys = redisTemplate.keys(RedisKeyConstants.MOVIE_PAGE_PREFIX + "*");
            if (detailKeys != null && !detailKeys.isEmpty()) redisTemplate.delete(detailKeys);
            if (pageKeys != null && !pageKeys.isEmpty()) redisTemplate.delete(pageKeys);
            redisTemplate.delete(RedisKeyConstants.GENRE_LIST);
            log.info("Invalidated {} detail + {} page cache keys",
                    detailKeys != null ? detailKeys.size() : 0,
                    pageKeys != null ? pageKeys.size() : 0);
        } catch (Exception e) {
            log.warn("Failed to invalidate movie cache", e);
        }
    }
}
