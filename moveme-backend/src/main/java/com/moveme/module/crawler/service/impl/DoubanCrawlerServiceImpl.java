package com.moveme.module.crawler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moveme.common.constant.CrawlerConstants;
import com.moveme.module.crawler.entity.CrawlLog;
import com.moveme.module.crawler.mapper.CrawlLogMapper;
import com.moveme.module.crawler.parser.DoubanMovieParser;
import com.moveme.module.crawler.service.CrawlerService;
import com.moveme.module.movie.entity.*;
import com.moveme.module.movie.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoubanCrawlerServiceImpl implements CrawlerService {

    private final DoubanMovieParser parser;
    private final MovieMapper movieMapper;
    private final GenreMapper genreMapper;
    private final MovieGenreMapper movieGenreMapper;
    private final MovieActorMapper movieActorMapper;
    private final MovieDirectorMapper movieDirectorMapper;
    private final CrawlLogMapper crawlLogMapper;

    @Value("${moveme.crawler.delay-min-ms:3000}")
    private int delayMinMs;

    @Value("${moveme.crawler.delay-max-ms:5000}")
    private int delayMaxMs;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    // ======================== 公开接口 ========================

    @Override
    public CrawlLog crawlByTag(String tag, int limit) {
        CrawlLog crawlLog = startLog(CrawlerConstants.TASK_TYPE_TAG + ":" + tag, limit);

        try {
            int successCount = 0;
            int failCount = 0;

            // 分页爬取列表
            for (int start = 0; start < limit; start += 50) {
                int pageLimit = Math.min(50, limit - start);
                String url = String.format(CrawlerConstants.DOUBAN_SEARCH_URL,
                        URLEncoder.encode(tag, StandardCharsets.UTF_8), pageLimit, start);

                String json = fetchUrl(url);
                if (json == null) {
                    failCount += pageLimit;
                    log.warn("爬取列表失败, tag={}, start={}", tag, start);
                    continue;
                }

                List<Movie> movies = parser.parseListJson(json);
                log.info("列表解析: tag={}, start={}, 获取 {} 部电影", tag, start, movies.size());

                // 逐部入库 + 爬详情
                for (Movie movie : movies) {
                    try {
                        saveMovieFromList(movie);
                        // 爬详情页，填充完整信息
                        crawlMovieDetail(movie.getDoubanId());
                        successCount++;
                    } catch (Exception e) {
                        failCount++;
                        log.warn("处理电影失败: {}", movie.getTitle(), e);
                    }
                }

                if (movies.isEmpty()) break; // 没有更多数据了
            }

            finishLog(crawlLog, CrawlerConstants.CRAWL_STATUS_SUCCESS, successCount, failCount, null);
        } catch (Exception e) {
            log.error("爬虫任务异常: tag={}", tag, e);
            finishLog(crawlLog, CrawlerConstants.CRAWL_STATUS_FAILED, 0, 0, e.getMessage());
        }

        return crawlLog;
    }

    @Override
    public CrawlLog crawlTop250() {
        // Top 250 可以用标签 "豆瓣高分" 或分页爬取
        return crawlByTag("豆瓣高分", 250);
    }

    @Override
    public void crawlMovieDetail(String doubanId) {
        String url = String.format(CrawlerConstants.DOUBAN_DETAIL_URL, doubanId);

        String html = fetchUrl(url);
        if (html == null) {
            log.warn("爬取详情页失败: doubanId={}", doubanId);
            return;
        }

        // 查找数据库中的电影记录
        Movie existingMovie = movieMapper.selectOne(
                new LambdaQueryWrapper<Movie>().eq(Movie::getDoubanId, doubanId));
        if (existingMovie == null) {
            existingMovie = new Movie();
            existingMovie.setDoubanId(doubanId);
        }

        // 解析详情页
        DoubanMovieParser.ParsedDetail detail = parser.parseDetailHtml(html, existingMovie);
        Movie movie = detail.getMovie();

        // 保存/更新电影
        movieMapper.upsertByDoubanId(movie);

        // 需要 movie.id，upsert 后 id 可能未回填，查一次
        if (movie.getId() == null) {
            Movie saved = movieMapper.selectOne(
                    new LambdaQueryWrapper<Movie>().eq(Movie::getDoubanId, doubanId));
            if (saved != null) {
                movie.setId(saved.getId());
            }
        }

        if (movie.getId() != null) {
            saveRelations(movie.getId(), detail);
        }

        log.debug("详情已保存: [{}] {}", doubanId, movie.getTitle());
    }

    @Override
    public List<CrawlLog> getCrawlLogs(int limit) {
        return crawlLogMapper.selectList(
                new LambdaQueryWrapper<CrawlLog>()
                        .orderByDesc(CrawlLog::getStartedAt)
                        .last("LIMIT " + limit));
    }

    // ======================== 内部方法 ========================

    /**
     * 发起 HTTP 请求（带反爬策略）
     * - 随机 User-Agent
     * - 模拟浏览器请求头
     * - 随机延迟 3-5 秒
     * - 403 返回 null
     */
    private String fetchUrl(String url) {
        randomDelay();

        String ua = CrawlerConstants.USER_AGENTS[
                ThreadLocalRandom.current().nextInt(CrawlerConstants.USER_AGENTS.length)];

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ua)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                // 不要手动设置 Accept-Encoding，OkHttp 会自动处理 gzip 解压
                .header("Referer", "https://movie.douban.com/")
                .header("Connection", "keep-alive")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 403 || response.code() == 418) {
                log.warn("被豆瓣限制 (HTTP {}), URL: {}", response.code(), url);
                // 退避：等 30 秒后再试
                Thread.sleep(30_000);
                return null;
            }
            if (!response.isSuccessful()) {
                log.warn("请求失败 (HTTP {}): {}", response.code(), url);
                return null;
            }
            return response.body() != null ? response.body().string() : null;
        } catch (IOException e) {
            log.error("网络请求异常: {}", url, e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 随机延迟（反爬核心）
     */
    private void randomDelay() {
        try {
            int delay = ThreadLocalRandom.current().nextInt(delayMinMs, delayMaxMs + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 保存列表接口获取的基础电影信息
     */
    private void saveMovieFromList(Movie movie) {
        movieMapper.upsertByDoubanId(movie);
    }

    /**
     * 保存电影关联数据（类型、导演、演员）
     */
    @Transactional
    private void saveRelations(Long movieId, DoubanMovieParser.ParsedDetail detail) {
        // 预加载所有类型，避免重复查询
        Map<String, Integer> genreMap = genreMapper.selectList(null).stream()
                .collect(Collectors.toMap(Genre::getName, Genre::getId, (a, b) -> a));

        // 清除旧关联后重新插入
        movieGenreMapper.deleteByMovieId(movieId);
        for (String genreName : detail.getGenres()) {
            Integer genreId = genreMap.get(genreName);
            if (genreId == null) {
                // 新类型，插入 genres 表
                Genre newGenre = new Genre();
                newGenre.setName(genreName);
                genreMapper.insert(newGenre);
                genreId = newGenre.getId();
            }
            movieGenreMapper.insertIgnore(movieId, genreId);
        }

        // 导演（先删后插）
        movieDirectorMapper.delete(
                new LambdaQueryWrapper<MovieDirector>().eq(MovieDirector::getMovieId, movieId));
        for (String name : detail.getDirectors()) {
            MovieDirector director = new MovieDirector();
            director.setMovieId(movieId);
            director.setName(name);
            movieDirectorMapper.insert(director);
        }

        // 演员（先删后插，保留顺序）
        movieActorMapper.delete(
                new LambdaQueryWrapper<MovieActor>().eq(MovieActor::getMovieId, movieId));
        for (int i = 0; i < detail.getActors().size(); i++) {
            MovieActor actor = new MovieActor();
            actor.setMovieId(movieId);
            actor.setName(detail.getActors().get(i));
            actor.setSortOrder(i);
            movieActorMapper.insert(actor);
        }
    }

    // ======================== 日志辅助 ========================

    private CrawlLog startLog(String taskType, int totalCount) {
        CrawlLog crawlLog = new CrawlLog();
        crawlLog.setTaskType(taskType);
        crawlLog.setStatus(CrawlerConstants.CRAWL_STATUS_RUNNING);
        crawlLog.setTotalCount(totalCount);
        crawlLog.setSuccessCount(0);
        crawlLog.setFailCount(0);
        crawlLog.setStartedAt(LocalDateTime.now());
        crawlLogMapper.insert(crawlLog);
        return crawlLog;
    }

    private void finishLog(CrawlLog crawlLog, int status, int successCount, int failCount, String error) {
        crawlLog.setStatus(status);
        crawlLog.setSuccessCount(successCount);
        crawlLog.setFailCount(failCount);
        crawlLog.setErrorMessage(error);
        crawlLog.setFinishedAt(LocalDateTime.now());
        crawlLogMapper.updateById(crawlLog);
        log.info("爬虫任务完成: type={}, 成功={}, 失败={}", crawlLog.getTaskType(), successCount, failCount);
    }
}
