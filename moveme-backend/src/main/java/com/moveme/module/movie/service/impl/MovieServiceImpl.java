package com.moveme.module.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.common.exception.BusinessException;
import com.moveme.common.result.ResultCode;
import com.moveme.common.util.TextEncodingRepairUtil;
import com.moveme.module.movie.dto.MovieQueryDTO;
import com.moveme.module.movie.entity.Genre;
import com.moveme.module.movie.entity.Movie;
import com.moveme.module.movie.mapper.GenreMapper;
import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.movie.mapper.PersonMapper;
import com.moveme.module.movie.service.MovieService;
import com.moveme.module.movie.vo.MovieDetailVO;
import com.moveme.module.movie.vo.MovieVO;
import com.moveme.module.movie.vo.PersonBriefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieMapper movieMapper;
    private final GenreMapper genreMapper;
    private final PersonMapper personMapper;
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
                .orderByDesc(Movie::getPopularityScore)
                .orderByDesc(Movie::getDoubanRating)
                .orderByDesc(Movie::getDoubanVotes);

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

    private IPage<MovieVO> searchWithFullText(String keyword, long page, long size) {
        int offset = Math.max(0, Math.toIntExact((page - 1) * size));
        int limit = Math.toIntExact(size);

        try {
            long total = movieMapper.countByKeyword(keyword);
            List<Movie> records = total > 0
                    ? movieMapper.fullTextSearch(keyword, offset, limit)
                    : Collections.emptyList();
            if (total > 0 || !records.isEmpty()) {
                return toMoviePage(page, size, total, records);
            }
        } catch (Exception e) {
            log.warn("Full text search failed, fallback to LIKE query: keyword={}", keyword, e);
        }

        Page<Movie> fallbackPage = movieMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Movie>()
                        .eq(Movie::getStatus, 1)
                        .and(w -> w.like(Movie::getTitle, keyword)
                                .or().like(Movie::getTitleCn, keyword)
                                .or().like(Movie::getTitleEn, keyword)
                                .or().like(Movie::getSummary, keyword))
                        .orderByDesc(Movie::getDoubanRating)
                        .orderByDesc(Movie::getDoubanVotes));
        return toMoviePage(fallbackPage);
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
}
