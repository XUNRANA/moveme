package com.moveme.module.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.common.exception.BusinessException;
import com.moveme.common.result.ResultCode;
import com.moveme.common.util.TextEncodingRepairUtil;
import com.moveme.module.movie.dto.MovieQueryDTO;
import com.moveme.module.movie.entity.CommentVote;
import com.moveme.module.movie.entity.Genre;
import com.moveme.module.movie.entity.Movie;
import com.moveme.module.movie.entity.MovieComment;
import com.moveme.module.movie.mapper.GenreMapper;
import com.moveme.module.movie.mapper.MovieAkaMapper;
import com.moveme.module.movie.mapper.MovieAwardMapper;
import com.moveme.module.movie.mapper.CommentVoteMapper;
import com.moveme.module.movie.mapper.MovieCommentMapper;
import com.moveme.module.movie.mapper.MovieGenreRankMapper;
import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.movie.mapper.MoviePlayLinkMapper;
import com.moveme.module.movie.mapper.MovieRatingDistMapper;
import com.moveme.module.movie.mapper.MovieRelatedMapper;
import com.moveme.module.movie.mapper.MovieReleaseDateMapper;
import com.moveme.module.movie.mapper.MovieTagMapper;
import com.moveme.module.movie.mapper.MovieTop250Mapper;
import com.moveme.module.movie.mapper.PersonMapper;
import com.moveme.module.movie.service.MovieService;
import com.moveme.module.user.entity.Rating;
import com.moveme.module.user.entity.User;
import com.moveme.module.user.mapper.RatingMapper;
import com.moveme.module.user.mapper.UserMapper;
import com.moveme.module.movie.vo.BoardVO;
import com.moveme.module.movie.vo.DiscoverSectionVO;
import com.moveme.module.movie.vo.MovieChartVO;
import com.moveme.module.movie.vo.MovieDetailVO;
import com.moveme.module.movie.vo.MovieVO;
import com.moveme.module.movie.vo.PersonBriefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MovieMapper movieMapper;
    private final JdbcTemplate jdbcTemplate;
    private final GenreMapper genreMapper;
    private final PersonMapper personMapper;
    private final MovieCommentMapper movieCommentMapper;
    private final CommentVoteMapper commentVoteMapper;
    private final MovieAwardMapper movieAwardMapper;
    private final MovieReleaseDateMapper movieReleaseDateMapper;
    private final MovieRelatedMapper movieRelatedMapper;
    private final MovieAkaMapper movieAkaMapper;
    private final MovieRatingDistMapper movieRatingDistMapper;
    private final MovieGenreRankMapper movieGenreRankMapper;
    private final MoviePlayLinkMapper moviePlayLinkMapper;
    private final MovieTagMapper movieTagMapper;
    private final MovieTop250Mapper movieTop250Mapper;
    private final UserMapper userMapper;
    private final RatingMapper ratingMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public IPage<MovieVO> listMovies(MovieQueryDTO query) {
        String cacheKey = RedisKeyConstants.MOVIE_PAGE_PREFIX + buildPageCacheHash(query);
        IPage<MovieVO> cached = getCachedPage(cacheKey);
        if (cached != null) {
            return normalizeMoviePage(cached);
        }

        Page<Movie> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<Movie>()
                .eq(Movie::getStatus, 1)
                .eq(query.getYear() != null, Movie::getYear, query.getYear())
                .ge(query.getRatingMin() != null, Movie::getDoubanRating, query.getRatingMin())
                .last("ORDER BY CASE WHEN douban_votes >= 1000000 THEN 1 " +
                        "WHEN douban_votes >= 500000 THEN 2 " +
                        "WHEN douban_votes >= 300000 THEN 3 " +
                        "WHEN douban_votes >= 100000 THEN 4 " +
                        "WHEN douban_votes >= 10000 THEN 5 " +
                        "WHEN douban_votes >= 5000 THEN 6 " +
                        "WHEN douban_votes >= 1000 THEN 7 " +
                        "WHEN douban_votes >= 100 THEN 8 " +
                        "ELSE 9 END, douban_rating DESC, douban_votes DESC");

        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Movie::getTitle, keyword)
                    .or().like(Movie::getTitleCn, keyword)
                    .or().like(Movie::getTitleEn, keyword)
                    .or().like(Movie::getSummary, keyword));
        }

        if (StringUtils.hasText(query.getGenre())) {
            List<Long> movieIds = movieMapper.selectMovieIdsByGenre(query.getGenre().trim());
            if (movieIds.isEmpty()) {
                return emptyPage(query.getPage(), query.getSize());
            }
            wrapper.in(Movie::getId, movieIds);
        }

        Page<Movie> result = movieMapper.selectPage(page, wrapper);
        Page<MovieVO> voPage = toMoviePage(result);
        cacheValue(cacheKey, voPage, 10, TimeUnit.MINUTES);
        return voPage;
    }

    @Override
    public IPage<MovieVO> searchMovies(String keyword, long page, long size) {
        String normalizedKeyword = keyword.trim();
        String cacheKey = RedisKeyConstants.MOVIE_PAGE_PREFIX + "search:" + buildPageCacheHash(
                normalizedKeyword, page, size);
        IPage<MovieVO> cached = getCachedPage(cacheKey);
        if (cached != null) {
            return normalizeMoviePage(cached);
        }

        IPage<MovieVO> result = searchWithFullText(normalizedKeyword, page, size);
        cacheValue(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public MovieDetailVO getMovieDetail(Long id) {
        String cacheKey = RedisKeyConstants.MOVIE_DETAIL_PREFIX + id;
        Object cached = getCachedValue(cacheKey);
        if (cached instanceof MovieDetailVO cachedVo) {
            return normalizeMovieDetail(cachedVo);
        }

        Movie movie = movieMapper.selectById(id);
        if (movie == null || !Objects.equals(movie.getStatus(), 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "movie not found");
        }

        MovieDetailVO detail = toMovieDetail(movie);
        cacheValue(cacheKey, detail, 30, TimeUnit.MINUTES);
        return detail;
    }

    @Override
    public List<String> listGenres() {
        Object cached = getCachedValue(RedisKeyConstants.GENRE_LIST);
        if (cached instanceof List<?> cachedList) {
            return cachedList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(TextEncodingRepairUtil::repairIfNeeded)
                    .distinct()
                    .sorted()
                    .toList();
        }

        List<String> genres = genreMapper.selectList(new LambdaQueryWrapper<Genre>()
                        .orderByAsc(Genre::getName))
                .stream()
                .map(Genre::getName)
                .map(TextEncodingRepairUtil::repairIfNeeded)
                .distinct()
                .sorted()
                .toList();
        cacheValue(RedisKeyConstants.GENRE_LIST, genres, 24, TimeUnit.HOURS);
        return genres;
    }

    @Override
    public List<String> listChartGenres() {
        String cacheKey = RedisKeyConstants.MOVIE_CHART_GENRES;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> cachedList && !cachedList.isEmpty()) {
                return cachedList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT genre_name FROM movie_chart WHERE board = 'type_rank' AND genre_name IS NOT NULL GROUP BY genre_name ORDER BY genre_name");
        List<String> genres = rows.stream()
                .map(m -> (String) m.get("genre_name"))
                .toList();
        try {
            redisTemplate.opsForValue().set(cacheKey, genres, 24, TimeUnit.HOURS);
        } catch (Exception ignored) {}
        return genres;
    }

    @Override
    public MovieChartVO getChartByGenre(String genre) {
        String cacheKey = RedisKeyConstants.MOVIE_CHART_PREFIX + genre;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof MovieChartVO cachedVo) {
                return cachedVo;
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT m.id AS movieId, m.title, m.poster_local_path AS posterUrl, " +
                "m.douban_rating AS rating, m.year, mc.rank_no AS rankNo " +
                "FROM movie_chart mc JOIN movies m ON m.id = mc.movie_id " +
                "WHERE mc.genre_name = ? AND m.status = 1 ORDER BY mc.rank_no ASC", genre);
        List<MovieChartVO.ChartMovieItem> items = rows.stream()
                .map(r -> mapToChartItem(r, false)).toList();
        MovieChartVO vo = new MovieChartVO();
        vo.setGenreName(genre);
        vo.setBoardTitle("分类排行榜-" + genre);
        vo.setMovies(items);
        try {
            redisTemplate.opsForValue().set(cacheKey, vo, 30, TimeUnit.MINUTES);
        } catch (Exception ignored) {}
        return vo;
    }

    @Override
    public List<Integer> listAnnualYears() {
        String cacheKey = RedisKeyConstants.MOVIE_ANNUAL_YEARS;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> cachedList && !cachedList.isEmpty()) {
                return cachedList.stream()
                        .filter(Number.class::isInstance)
                        .map(n -> ((Number) n).intValue())
                        .toList();
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DISTINCT annual_year FROM movie_annual ORDER BY annual_year DESC");
        List<Integer> years = rows.stream()
                .map(m -> ((Number) m.get("annual_year")).intValue())
                .toList();
        try {
            redisTemplate.opsForValue().set(cacheKey, years, 24, TimeUnit.HOURS);
        } catch (Exception ignored) {}
        return years;
    }

    @Override
    public List<MovieChartVO> getAnnualByYear(Integer year) {
        String cacheKey = RedisKeyConstants.MOVIE_ANNUAL_PREFIX + year;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> cachedList && !cachedList.isEmpty()) {
                return cachedList.stream()
                        .filter(MovieChartVO.class::isInstance)
                        .map(MovieChartVO.class::cast)
                        .toList();
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ma.board_title AS boardTitle, ma.rank_no AS rankNo, " +
                "m.id AS movieId, m.title, m.poster_local_path AS posterUrl, " +
                "m.douban_rating AS rating, m.year " +
                "FROM movie_annual ma JOIN movies m ON m.id = ma.movie_id " +
                "WHERE ma.annual_year = ? AND m.status = 1 " +
                "ORDER BY ma.board_order, ma.rank_no ASC", year);

        Map<String, List<MovieChartVO.ChartMovieItem>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String boardTitle = (String) r.get("boardTitle");
            grouped.computeIfAbsent(boardTitle, k -> new ArrayList<>()).add(mapToChartItem(r, false));
        }

        List<MovieChartVO> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            MovieChartVO vo = new MovieChartVO();
            vo.setGenreName(entry.getKey());
            vo.setBoardTitle(entry.getKey());
            vo.setMovies(entry.getValue());
            result.add(vo);
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);
        } catch (Exception ignored) {}
        return result;
    }

    @Override
    public List<BoardVO> listBoards() {
        String cacheKey = RedisKeyConstants.MOVIE_BOARDS;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> cachedList && !cachedList.isEmpty()) {
                return cachedList.stream()
                        .filter(BoardVO.class::isInstance)
                        .map(BoardVO.class::cast)
                        .toList();
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT board, genre_name, board_title FROM movie_chart " +
                "WHERE board NOT IN ('type_rank', 'top250') " +
                "GROUP BY board, genre_name, board_title " +
                "ORDER BY FIELD(board, 'weekly_word_of_mouth', 'north_america_box_office', 'new_movies')");

        Map<String, String> displayNames = Map.of(
                "weekly_word_of_mouth", "一周口碑",
                "north_america_box_office", "北美票房",
                "new_movies", "新片榜"
        );

        List<BoardVO> boards = rows.stream().map(r -> {
            BoardVO vo = new BoardVO();
            String board = (String) r.get("board");
            vo.setBoardName(board);
            vo.setDisplayName(displayNames.getOrDefault(board, board));
            vo.setGenreName((String) r.get("genre_name"));
            vo.setBoardTitle((String) r.get("board_title"));
            return vo;
        }).toList();

        try {
            redisTemplate.opsForValue().set(cacheKey, boards, 24, TimeUnit.HOURS);
        } catch (Exception ignored) {}
        return boards;
    }

    @Override
    public MovieChartVO listTop250() {
        String cacheKey = RedisKeyConstants.MOVIE_TOP250;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof MovieChartVO cachedVo) {
                return cachedVo;
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT t.movie_id AS movieId, COALESCE(m.title_cn, m.title) AS title, " +
                "COALESCE(m.poster_local_path, m.poster_url) AS posterUrl, " +
                "m.douban_rating AS rating, m.year, t.rank_no AS rankNo " +
                "FROM movie_top250 t JOIN movies m ON m.id = t.movie_id " +
                "WHERE m.status = 1 ORDER BY t.rank_no ASC");
        List<MovieChartVO.ChartMovieItem> items = rows.stream()
                .map(r -> mapToChartItem(r, true)).toList();

        MovieChartVO vo = new MovieChartVO();
        vo.setGenreName("Top250");
        vo.setBoardTitle("豆瓣 Top 250");
        vo.setMovies(items);
        try {
            redisTemplate.opsForValue().set(cacheKey, vo, 30, TimeUnit.MINUTES);
        } catch (Exception ignored) {}
        return vo;
    }

    @Override
    public MovieChartVO getBoardMovies(String boardName) {
        String cacheKey = RedisKeyConstants.MOVIE_BOARD_MOVIES_PREFIX + boardName;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof MovieChartVO cachedVo) {
                return cachedVo;
            }
        } catch (Exception ignored) {}

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT m.id AS movieId, m.title, " +
                "COALESCE(m.poster_local_path, m.poster_url) AS posterUrl, " +
                "m.douban_rating AS rating, m.year, mc.rank_no AS rankNo " +
                "FROM movie_chart mc JOIN movies m ON m.id = mc.movie_id " +
                "WHERE mc.board = ? AND m.status = 1 ORDER BY mc.rank_no ASC", boardName);

        Map<String, String> displayNames = Map.of(
                "weekly_word_of_mouth", "一周口碑电影榜",
                "north_america_box_office", "北美票房榜",
                "new_movies", "豆瓣新片榜"
        );

        List<MovieChartVO.ChartMovieItem> items = rows.stream()
                .map(r -> mapToChartItem(r, true)).toList();

        MovieChartVO vo = new MovieChartVO();
        vo.setGenreName(boardName);
        vo.setBoardTitle(displayNames.getOrDefault(boardName, boardName));
        vo.setMovies(items);
        try {
            redisTemplate.opsForValue().set(cacheKey, vo, 30, TimeUnit.MINUTES);
        } catch (Exception ignored) {}
        return vo;
    }

    @Override
    public List<DiscoverSectionVO> discover() {
        String cacheKey = RedisKeyConstants.MOVIE_DISCOVER;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> cachedList && !cachedList.isEmpty()) {
                return cachedList.stream()
                        .filter(DiscoverSectionVO.class::isInstance)
                        .map(DiscoverSectionVO.class::cast)
                        .toList();
            }
        } catch (Exception ignored) {}

        List<DiscoverSectionVO> sections = new ArrayList<>();

        // 1. 热门精选 — top by votes
        List<MovieVO> popular = queryMovies(
                "SELECT * FROM movies WHERE status = 1 ORDER BY douban_votes DESC LIMIT 20");
        if (!popular.isEmpty()) {
            DiscoverSectionVO s = new DiscoverSectionVO();
            s.setKey("popular");
            s.setTitle("热门精选");
            s.setMovies(popular);
            sections.add(s);
        }

        // 2. Genre sections — top 6 genres (from movie_chart, same source as /charts)
        String[] topGenres = {"剧情", "喜剧", "动作", "爱情", "科幻", "动画"};
        for (String genre : topGenres) {
            List<MovieVO> genreMovies = queryMovies(
                    "SELECT m.* FROM movies m " +
                    "JOIN movie_chart mc ON m.id = mc.movie_id " +
                    "WHERE mc.genre_name = ? AND mc.board = 'type_rank' AND m.status = 1 " +
                    "ORDER BY mc.rank_no ASC LIMIT 15", genre);
            if (!genreMovies.isEmpty()) {
                DiscoverSectionVO s = new DiscoverSectionVO();
                s.setKey("genre_" + genre);
                s.setTitle(genre + " · 精选");
                s.setMovies(genreMovies);
                sections.add(s);
            }
        }

        // 3. 冷门佳作 — high rating, lower votes
        List<MovieVO> hiddenGems = queryMovies(
                "SELECT * FROM movies WHERE status = 1 AND douban_rating >= 8.5 " +
                "AND douban_votes < 50000 ORDER BY douban_rating DESC LIMIT 15");
        if (!hiddenGems.isEmpty()) {
            DiscoverSectionVO s = new DiscoverSectionVO();
            s.setKey("hidden_gem");
            s.setTitle("冷门佳作");
            s.setMovies(hiddenGems);
            sections.add(s);
        }

        // 4. 经典老片 — before 2000
        List<MovieVO> classics = queryMovies(
                "SELECT * FROM movies WHERE status = 1 AND year < 2000 " +
                "ORDER BY douban_rating DESC, douban_votes DESC LIMIT 15");
        if (!classics.isEmpty()) {
            DiscoverSectionVO s = new DiscoverSectionVO();
            s.setKey("classics");
            s.setTitle("经典老片");
            s.setMovies(classics);
            sections.add(s);
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, sections, 1, TimeUnit.HOURS);
        } catch (Exception ignored) {}
        return sections;
    }

    private List<MovieVO> queryMovies(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.stream().map(this::mapRowToMovieVO).toList();
    }

    private MovieVO mapRowToMovieVO(Map<String, Object> r) {
        MovieVO vo = new MovieVO();
        vo.setId(((Number) r.get("id")).longValue());
        vo.setDoubanId((String) r.get("douban_id"));
        vo.setTitle(TextEncodingRepairUtil.repairIfNeeded((String) r.get("title")));
        vo.setTitleCn(TextEncodingRepairUtil.repairIfNeeded((String) r.get("title_cn")));
        vo.setTitleEn(TextEncodingRepairUtil.repairIfNeeded((String) r.get("title_en")));
        vo.setPosterUrl((String) r.get("poster_url"));
        vo.setPosterLocalPath((String) r.get("poster_local_path"));
        vo.setYear(r.get("year") != null ? ((Number) r.get("year")).shortValue() : null);
        vo.setDoubanRating(r.get("douban_rating") != null ? new java.math.BigDecimal(r.get("douban_rating").toString()) : null);
        vo.setDoubanVotes(r.get("douban_votes") != null ? ((Number) r.get("douban_votes")).intValue() : null);
        vo.setLocalRating(r.get("local_rating") != null ? new java.math.BigDecimal(r.get("local_rating").toString()) : null);
        vo.setLocalVotes(r.get("local_votes") != null ? ((Number) r.get("local_votes")).intValue() : null);
        vo.setWishCount(r.get("wish_count") != null ? ((Number) r.get("wish_count")).intValue() : null);
        vo.setCollectCount(r.get("collect_count") != null ? ((Number) r.get("collect_count")).intValue() : null);
        vo.setPopularityScore(r.get("popularity_score") != null ? new java.math.BigDecimal(r.get("popularity_score").toString()) : null);
        vo.setSummary(TextEncodingRepairUtil.repairIfNeeded((String) r.get("summary")));
        vo.setSummaryShort(TextEncodingRepairUtil.repairIfNeeded((String) r.get("summary_short")));
        vo.setDurationText(TextEncodingRepairUtil.repairIfNeeded((String) r.get("duration_text")));
        // Load genres
        try {
            vo.setGenres(movieMapper.selectGenreNamesByMovieId(vo.getId()).stream()
                    .map(TextEncodingRepairUtil::repairIfNeeded)
                    .distinct().sorted().toList());
        } catch (Exception e) {
            vo.setGenres(List.of());
        }
        return vo;
    }

    private IPage<MovieVO> searchWithFullText(String keyword, long page, long size) {
        // 优先按标题匹配（LIKE 精确匹配，中文友好）
        Page<Movie> titlePage = movieMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Movie>()
                        .eq(Movie::getStatus, 1)
                        .and(w -> w.like(Movie::getTitleCn, keyword)
                                .or().like(Movie::getTitle, keyword)
                                .or().like(Movie::getTitleEn, keyword))
                        .orderByDesc(Movie::getDoubanRating)
                        .orderByDesc(Movie::getDoubanVotes));
        if (titlePage.getTotal() > 0) {
            return toMoviePage(titlePage);
        }

        // 标题无结果时，回退到简介搜索
        Page<Movie> summaryPage = movieMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Movie>()
                        .eq(Movie::getStatus, 1)
                        .like(Movie::getSummary, keyword)
                        .orderByDesc(Movie::getDoubanRating)
                        .orderByDesc(Movie::getDoubanVotes));
        return toMoviePage(summaryPage);
    }

    private Page<MovieVO> toMoviePage(Page<Movie> page) {
        return toMoviePage(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    private Page<MovieVO> toMoviePage(long current, long size, long total, List<Movie> movies) {
        Page<MovieVO> page = new Page<>(current, size, total);
        page.setRecords(movies.stream().map(this::toMovieVO).toList());
        return page;
    }

    private Page<MovieVO> emptyPage(long current, long size) {
        return new Page<>(current, size, 0);
    }

    private MovieVO toMovieVO(Movie movie) {
        MovieVO vo = new MovieVO();
        vo.setId(movie.getId());
        vo.setDoubanId(movie.getDoubanId());
        vo.setTitle(TextEncodingRepairUtil.repairIfNeeded(movie.getTitle()));
        vo.setTitleCn(TextEncodingRepairUtil.repairIfNeeded(movie.getTitleCn()));
        vo.setTitleEn(TextEncodingRepairUtil.repairIfNeeded(movie.getTitleEn()));
        vo.setPosterUrl(movie.getPosterUrl());
        vo.setPosterLocalPath(movie.getPosterLocalPath());
        vo.setYear(movie.getYear());
        vo.setDoubanRating(movie.getDoubanRating());
        vo.setDoubanVotes(movie.getDoubanVotes());
        vo.setLocalRating(movie.getLocalRating());
        vo.setLocalVotes(movie.getLocalVotes());
        vo.setWishCount(movie.getWishCount());
        vo.setCollectCount(movie.getCollectCount());
        vo.setPopularityScore(movie.getPopularityScore());
        vo.setSummary(TextEncodingRepairUtil.repairIfNeeded(movie.getSummary()));
        vo.setSummaryShort(TextEncodingRepairUtil.repairIfNeeded(movie.getSummaryShort()));
        vo.setDurationText(TextEncodingRepairUtil.repairIfNeeded(movie.getDurationText()));
        vo.setGenres(movieMapper.selectGenreNamesByMovieId(movie.getId()).stream()
                .map(TextEncodingRepairUtil::repairIfNeeded)
                .distinct()
                .sorted()
                .toList());
        return vo;
    }

    private MovieDetailVO toMovieDetail(Movie movie) {
        MovieDetailVO detail = new MovieDetailVO();
        MovieVO base = toMovieVO(movie);
        detail.setId(base.getId());
        detail.setDoubanId(base.getDoubanId());
        detail.setTitle(base.getTitle());
        detail.setTitleCn(base.getTitleCn());
        detail.setTitleEn(base.getTitleEn());
        detail.setPosterUrl(base.getPosterUrl());
        detail.setPosterLocalPath(base.getPosterLocalPath());
        detail.setYear(base.getYear());
        detail.setDoubanRating(base.getDoubanRating());
        detail.setDoubanVotes(base.getDoubanVotes());
        detail.setLocalRating(base.getLocalRating());
        detail.setLocalVotes(base.getLocalVotes());
        detail.setWishCount(base.getWishCount());
        detail.setCollectCount(base.getCollectCount());
        detail.setPopularityScore(base.getPopularityScore());
        detail.setSummary(base.getSummary());
        detail.setSummaryShort(base.getSummaryShort());
        detail.setDurationText(base.getDurationText());
        detail.setGenres(base.getGenres());

        detail.setReleaseDate(movie.getReleaseDate());
        detail.setImdbId(movie.getImdbId());
        detail.setCountries(movieMapper.selectCountryNamesByMovieId(movie.getId()).stream()
                .map(TextEncodingRepairUtil::repairIfNeeded).toList());
        detail.setLanguages(movieMapper.selectLanguageNamesByMovieId(movie.getId()).stream()
                .map(TextEncodingRepairUtil::repairIfNeeded).toList());
        detail.setDirectors(repairPersonNames(personMapper.selectDirectorsByMovieId(movie.getId())));
        detail.setActors(repairPersonNames(personMapper.selectActorsByMovieId(movie.getId())));
        detail.setWriters(repairPersonNames(personMapper.selectWritersByMovieId(movie.getId())));

        // Phase B enriched data
        Long movieId = movie.getId();
        try {
            detail.setAkas(movieAkaMapper.selectTitlesByMovieId(movieId));
        } catch (Exception e) { detail.setAkas(List.of()); }
        try {
            detail.setTags(movieTagMapper.selectTagNamesByMovieId(movieId));
        } catch (Exception e) { detail.setTags(List.of()); }
        try {
            detail.setReleaseDates(movieReleaseDateMapper.selectByMovieId(movieId).stream()
                    .map(r -> {
                        MovieDetailVO.ReleaseDateVO vo = new MovieDetailVO.ReleaseDateVO();
                        vo.setDate(r.getReleaseAt());
                        vo.setRegion(r.getRegion());
                        vo.setRawText(r.getRawText());
                        return vo;
                    }).toList());
        } catch (Exception e) { detail.setReleaseDates(List.of()); }
        try {
            detail.setAwards(movieAwardMapper.selectByMovieId(movieId).stream()
                    .map(a -> {
                        MovieDetailVO.AwardVO vo = new MovieDetailVO.AwardVO();
                        vo.setCeremony(a.getCeremonyText());
                        vo.setCategory(a.getCategory());
                        vo.setStatus(a.getStatus());
                        vo.setRecipient(a.getRecipientText());
                        vo.setUrl(a.getAwardUrl());
                        return vo;
                    }).toList());
        } catch (Exception e) { detail.setAwards(List.of()); }
        try {
            detail.setRelatedMovies(movieRelatedMapper.selectByMovieId(movieId).stream()
                    .map(r -> {
                        MovieDetailVO.RelatedMovieVO vo = new MovieDetailVO.RelatedMovieVO();
                        vo.setMovieId(r.getRelatedMovieId());
                        vo.setTitle(r.getRelatedTitle());
                        vo.setRating(r.getRelatedRating());
                        // 优先用本地海报，豆瓣 URL 直接加载会被防盗链 403
                        if (r.getRelatedMovieId() != null) {
                            Movie rel = movieMapper.selectById(r.getRelatedMovieId());
                            if (rel != null && rel.getPosterLocalPath() != null) {
                                vo.setCoverUrl(rel.getPosterLocalPath());
                            } else {
                                vo.setCoverUrl(r.getRelatedCoverUrl());
                            }
                        } else {
                            vo.setCoverUrl(r.getRelatedCoverUrl());
                        }
                        return vo;
                    }).toList());
        } catch (Exception e) { detail.setRelatedMovies(List.of()); }
        try {
            detail.setRatingDist(movieRatingDistMapper.selectByMovieId(movieId).stream()
                    .map(d -> {
                        MovieDetailVO.RatingDistVO vo = new MovieDetailVO.RatingDistVO();
                        vo.setStar(d.getStar());
                        vo.setLabel(d.getLabel());
                        vo.setPercentage(d.getPercentage());
                        return vo;
                    }).toList());
        } catch (Exception e) { detail.setRatingDist(List.of()); }
        try {
            detail.setGenreRanks(movieGenreRankMapper.selectByMovieId(movieId).stream()
                    .map(gr -> {
                        MovieDetailVO.GenreRankVO vo = new MovieDetailVO.GenreRankVO();
                        vo.setGenre(gr.getGenreName());
                        vo.setPercentile(gr.getPercentile());
                        return vo;
                    }).toList());
        } catch (Exception e) { detail.setGenreRanks(List.of()); }
        try {
            var top250 = movieTop250Mapper.selectByMovieId(movieId);
            if (top250 != null) {
                MovieDetailVO.Top250VO vo = new MovieDetailVO.Top250VO();
                vo.setRank(top250.getRankNo() != null ? top250.getRankNo().intValue() : null);
                vo.setListTitle(top250.getListTitle());
                vo.setQuote(top250.getQuote());
                detail.setTop250(vo);
            }
        } catch (Exception e) { detail.setTop250(null); }
        try {
            detail.setPlayLinks(moviePlayLinkMapper.selectByMovieId(movieId).stream()
                    .map(pl -> {
                        MovieDetailVO.PlayLinkVO vo = new MovieDetailVO.PlayLinkVO();
                        vo.setPlatform(TextEncodingRepairUtil.repairIfNeeded(pl.getPlatform()));
                        vo.setUrl(pl.getUrl());
                        return vo;
                    }).toList());
        } catch (Exception e) { detail.setPlayLinks(List.of()); }

        return detail;
    }

    private List<PersonBriefVO> repairPersonNames(List<PersonBriefVO> list) {
        if (list == null) return Collections.emptyList();
        for (PersonBriefVO p : list) {
            p.setName(TextEncodingRepairUtil.repairIfNeeded(p.getName()));
            p.setRoleName(TextEncodingRepairUtil.repairIfNeeded(p.getRoleName()));
        }
        return list;
    }

    private IPage<MovieVO> normalizeMoviePage(IPage<MovieVO> page) {
        if (page == null || page.getRecords() == null) {
            return page;
        }
        page.setRecords(page.getRecords().stream()
                .map(this::normalizeMovieVO)
                .toList());
        return page;
    }

    private MovieVO normalizeMovieVO(MovieVO vo) {
        if (vo == null) {
            return null;
        }
        vo.setTitle(TextEncodingRepairUtil.repairIfNeeded(vo.getTitle()));
        vo.setTitleCn(TextEncodingRepairUtil.repairIfNeeded(vo.getTitleCn()));
        vo.setTitleEn(TextEncodingRepairUtil.repairIfNeeded(vo.getTitleEn()));
        vo.setSummary(TextEncodingRepairUtil.repairIfNeeded(vo.getSummary()));
        vo.setSummaryShort(TextEncodingRepairUtil.repairIfNeeded(vo.getSummaryShort()));
        vo.setDurationText(TextEncodingRepairUtil.repairIfNeeded(vo.getDurationText()));
        if (vo.getGenres() != null) {
            vo.setGenres(vo.getGenres().stream()
                    .map(TextEncodingRepairUtil::repairIfNeeded)
                    .distinct()
                    .sorted()
                    .toList());
        }
        return vo;
    }

    private MovieDetailVO normalizeMovieDetail(MovieDetailVO detail) {
        if (detail == null) {
            return null;
        }
        normalizeMovieVO(detail);
        if (detail.getCountries() != null) {
            detail.setCountries(detail.getCountries().stream()
                    .map(TextEncodingRepairUtil::repairIfNeeded).toList());
        }
        if (detail.getLanguages() != null) {
            detail.setLanguages(detail.getLanguages().stream()
                    .map(TextEncodingRepairUtil::repairIfNeeded).toList());
        }
        if (detail.getDirectors() != null) repairPersonNames(detail.getDirectors());
        if (detail.getActors() != null) repairPersonNames(detail.getActors());
        if (detail.getWriters() != null) repairPersonNames(detail.getWriters());
        return detail;
    }

    @SuppressWarnings("unchecked")
    private IPage<MovieVO> getCachedPage(String cacheKey) {
        Object cached = getCachedValue(cacheKey);
        if (cached instanceof IPage<?>) {
            return (IPage<MovieVO>) cached;
        }
        return null;
    }

    private Object getCachedValue(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Read cache failed: key={}", cacheKey, e);
            return null;
        }
    }

    private void cacheValue(String cacheKey, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Write cache failed: key={}", cacheKey, e);
        }
    }

    private String buildPageCacheHash(MovieQueryDTO query) {
        return buildPageCacheHash(
                query.getGenre(),
                query.getYear(),
                query.getRatingMin(),
                query.getKeyword(),
                query.getPage(),
                query.getSize());
    }

    private String buildPageCacheHash(Object... values) {
        return Integer.toHexString(Objects.hash(values));
    }

    private MovieChartVO.ChartMovieItem mapToChartItem(Map<String, Object> r, boolean repairTitle) {
        MovieChartVO.ChartMovieItem item = new MovieChartVO.ChartMovieItem();
        item.setMovieId(((Number) r.get("movieId")).longValue());
        String title = (String) r.get("title");
        item.setTitle(repairTitle ? TextEncodingRepairUtil.repairIfNeeded(title) : title);
        item.setPosterUrl((String) r.get("posterUrl"));
        item.setRating(r.get("rating") != null ? new java.math.BigDecimal(r.get("rating").toString()) : null);
        item.setYear(r.get("year") != null ? ((Number) r.get("year")).shortValue() : null);
        item.setRankNo(r.get("rankNo") != null ? ((Number) r.get("rankNo")).intValue() : null);
        return item;
    }

    // ─── 评论 ───

    @Override
    public IPage<MovieDetailVO.CommentVO> getMovieComments(Long movieId, long page, long size, String sort, Long userId) {
        Page<MovieComment> pageParam = new Page<>(page, size);
        List<MovieComment> comments = "new".equals(sort)
                ? movieCommentMapper.selectByMovieIdNew(pageParam, movieId)
                : movieCommentMapper.selectByMovieIdHot(pageParam, movieId);

        // 批量查询当前用户的点赞状态
        List<Long> commentIds = comments.stream().map(MovieComment::getId).toList();
        final java.util.Set<Long> likedIds;
        if (userId != null && !commentIds.isEmpty()) {
            List<CommentVote> votes = commentVoteMapper.selectList(
                    new LambdaQueryWrapper<CommentVote>()
                            .eq(CommentVote::getUserId, userId)
                            .in(CommentVote::getCommentId, commentIds));
            likedIds = votes.stream().map(CommentVote::getCommentId).collect(java.util.stream.Collectors.toSet());
        } else {
            likedIds = java.util.Set.of();
        }

        List<MovieDetailVO.CommentVO> voList = comments.stream().map(c -> {
            MovieDetailVO.CommentVO vo = new MovieDetailVO.CommentVO();
            vo.setId(c.getId());
            vo.setAuthorName(c.getAuthorName());
            vo.setAuthorAvatar(c.getAuthorAvatar());
            vo.setAuthorLocation(c.getAuthorLocation());
            vo.setRating(c.getRating());
            vo.setRatingLabel(c.getRatingLabel());
            vo.setContent(c.getContent());
            vo.setVotes(c.getVotes());
            vo.setPostedAt(c.getPostedAt() != null ? c.getPostedAt().format(DATE_FMT) : null);
            vo.setSourceUrl(c.getSourceUrl());
            vo.setLiked(userId != null && likedIds.contains(c.getId()));
            return vo;
        }).toList();

        Page<MovieDetailVO.CommentVO> result = new Page<>(page, size, pageParam.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Override
    public void submitComment(Long userId, Long movieId, String content, Integer rating) {
        User user = userMapper.selectById(userId);
        MovieComment comment = new MovieComment();
        comment.setMovieId(movieId);
        comment.setSource(1);
        comment.setUserId(userId);
        comment.setAuthorName(user != null && user.getNickname() != null ? user.getNickname() : user != null ? user.getUsername() : "匿名用户");
        comment.setContent(content);
        comment.setRating(rating);
        comment.setVotes(0);
        comment.setPostedAt(java.time.LocalDateTime.now());
        movieCommentMapper.insert(comment);

        // 同步更新 ratings 表的评分
        if (rating != null && rating > 0) {
            Rating existing = ratingMapper.selectOne(
                    new LambdaQueryWrapper<Rating>()
                            .eq(Rating::getUserId, userId)
                            .eq(Rating::getMovieId, movieId));
            if (existing != null) {
                existing.setScore(rating);
                existing.setComment(content);
                ratingMapper.updateById(existing);
            } else {
                Rating r = new Rating();
                r.setUserId(userId);
                r.setMovieId(movieId);
                r.setScore(rating);
                r.setComment(content);
                ratingMapper.insert(r);
            }
        }
    }

    @Override
    public void likeComment(Long userId, Long commentId) {
        // 检查是否已点赞
        Long exists = commentVoteMapper.selectCount(
                new LambdaQueryWrapper<CommentVote>()
                        .eq(CommentVote::getCommentId, commentId)
                        .eq(CommentVote::getUserId, userId));
        if (exists > 0) return;

        // 插入点赞记录
        CommentVote vote = new CommentVote();
        vote.setCommentId(commentId);
        vote.setUserId(userId);
        commentVoteMapper.insert(vote);

        // 评论点赞数 +1
        movieCommentMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<MovieComment>()
                        .eq("id", commentId)
                        .setSql("votes = votes + 1"));
    }

    @Override
    public void unlikeComment(Long userId, Long commentId) {
        int deleted = commentVoteMapper.delete(
                new LambdaQueryWrapper<CommentVote>()
                        .eq(CommentVote::getCommentId, commentId)
                        .eq(CommentVote::getUserId, userId));
        if (deleted > 0) {
            // 评论点赞数 -1
            movieCommentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<MovieComment>()
                            .eq("id", commentId)
                            .setSql("votes = GREATEST(votes - 1, 0)"));
        }
    }
}
