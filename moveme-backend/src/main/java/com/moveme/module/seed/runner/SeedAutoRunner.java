package com.moveme.module.seed.runner;

import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.seed.service.SeedImportService;
import com.moveme.module.seed.support.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动时自动导入 top250.json：
 *  - 仅在 moveme.seed.auto-import=true 时存在（@ConditionalOnProperty）
 *  - 仅在 movies 表行数 < 50 时跑（避免每次启动重复导入）
 *
 * 调试：把 application.yml 里 moveme.seed.auto-import 设 true。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "moveme.seed.auto-import", havingValue = "true")
public class SeedAutoRunner implements ApplicationRunner {

    private final SeedImportService seedImportService;
    private final MovieMapper movieMapper;

    @Override
    public void run(ApplicationArguments args) {
        long count = movieMapper.selectCount(null);
        if (count >= 50) {
            log.info("Skip seed import: movies table already has {} rows (>=50)", count);
            return;
        }
        log.info("Top250 seed import starting (movies count = {})", count);
        ImportResult r = seedImportService.importAll();
        log.info("Top250 seed import finished: total={} ok={} fail={} persons={} comments={} elapsed={}ms",
                r.total(), r.success(), r.fail(), r.personsUpserted(), r.commentsImported(), r.elapsedMs());
        if (!r.errors().isEmpty()) {
            log.warn("Top250 seed import had {} errors. First 5: {}",
                    r.errors().size(), r.errors().subList(0, Math.min(5, r.errors().size())));
        }
    }
}
