package com.moveme.module.seed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moveme.common.util.TextEncodingRepairUtil;
import com.moveme.module.movie.entity.AwardCeremony;
import com.moveme.module.movie.entity.Country;
import com.moveme.module.movie.entity.Genre;
import com.moveme.module.movie.entity.Language;
import com.moveme.module.movie.entity.Movie;
import com.moveme.module.movie.entity.MovieActor;
import com.moveme.module.movie.entity.MovieAka;
import com.moveme.module.movie.entity.MovieAward;
import com.moveme.module.movie.entity.MovieComment;
import com.moveme.module.movie.entity.MovieCountry;
import com.moveme.module.movie.entity.MovieDirector;
import com.moveme.module.movie.entity.MovieGenre;
import com.moveme.module.movie.entity.MovieGenreRank;
import com.moveme.module.movie.entity.MovieLanguage;
import com.moveme.module.movie.entity.MoviePlayLink;
import com.moveme.module.movie.entity.MovieRatingDist;
import com.moveme.module.movie.entity.MovieRelated;
import com.moveme.module.movie.entity.MovieReleaseDate;
import com.moveme.module.movie.entity.MovieTag;
import com.moveme.module.movie.entity.MovieTop250;
import com.moveme.module.movie.entity.MovieWriter;
import com.moveme.module.movie.entity.Person;
import com.moveme.module.movie.entity.Tag;
import com.moveme.module.movie.mapper.AwardCeremonyMapper;
import com.moveme.module.movie.mapper.CountryMapper;
import com.moveme.module.movie.mapper.GenreMapper;
import com.moveme.module.movie.mapper.LanguageMapper;
import com.moveme.module.movie.mapper.MovieActorMapper;
import com.moveme.module.movie.mapper.MovieAkaMapper;
import com.moveme.module.movie.mapper.MovieAwardMapper;
import com.moveme.module.movie.mapper.MovieCommentMapper;
import com.moveme.module.movie.mapper.MovieCountryMapper;
import com.moveme.module.movie.mapper.MovieDirectorMapper;
import com.moveme.module.movie.mapper.MovieGenreMapper;
import com.moveme.module.movie.mapper.MovieGenreRankMapper;
import com.moveme.module.movie.mapper.MovieLanguageMapper;
import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.movie.mapper.MoviePlayLinkMapper;
import com.moveme.module.movie.mapper.MovieRatingDistMapper;
import com.moveme.module.movie.mapper.MovieRelatedMapper;
import com.moveme.module.movie.mapper.MovieReleaseDateMapper;
import com.moveme.module.movie.mapper.MovieTagMapper;
import com.moveme.module.movie.mapper.MovieTop250Mapper;
import com.moveme.module.movie.mapper.MovieWriterMapper;
import com.moveme.module.movie.mapper.PersonMapper;
import com.moveme.module.movie.mapper.TagMapper;
import com.moveme.module.seed.dto.SeedAwardDTO;
import com.moveme.module.seed.dto.SeedCommentDTO;
import com.moveme.module.seed.dto.SeedMovieDTO;
import com.moveme.module.seed.dto.SeedPersonRefDTO;
import com.moveme.module.seed.dto.SeedRatingBetterThanDTO;
import com.moveme.module.seed.dto.SeedRatingBreakdownDTO;
import com.moveme.module.seed.dto.SeedPlayLinkDTO;
import com.moveme.module.seed.dto.SeedRelatedMovieDTO;
import com.moveme.module.seed.entity.ImportLog;
import com.moveme.module.seed.mapper.ImportLogMapper;
import com.moveme.module.seed.service.SeedImportService;
import com.moveme.module.seed.support.ImportContext;
import com.moveme.module.seed.support.ImportResult;
import com.moveme.module.seed.util.PosterFileImporter;
import com.moveme.module.seed.util.SeedTextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Top250 JSON 种子导入器（完整实现）。
 *
 * 详细文档见：
 *   - docs/11-Phase-B-种子导入器实现指南.md（设计 + 流程）
 *   - docs/13-Phase-B-业务逻辑实现细节.md（每个方法的字段映射 + 算法）
 *   - docs/14-Phase-B-代码实现讲解.md（本类逐方法讲解：为什么这么写）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Top250SeedImportServiceImpl implements SeedImportService {

    private final MovieMapper movieMapper;
    private final PersonMapper personMapper;
    private final GenreMapper genreMapper;
    private final CountryMapper countryMapper;
    private final LanguageMapper languageMapper;
    private final TagMapper tagMapper;
    private final AwardCeremonyMapper awardCeremonyMapper;
    private final MovieGenreMapper movieGenreMapper;
    private final MovieCountryMapper movieCountryMapper;
    private final MovieLanguageMapper movieLanguageMapper;
    private final MovieTagMapper movieTagMapper;
    private final MovieDirectorMapper movieDirectorMapper;
    private final MovieWriterMapper movieWriterMapper;
    private final MovieActorMapper movieActorMapper;
    private final MovieAkaMapper movieAkaMapper;
    private final MovieReleaseDateMapper movieReleaseDateMapper;
    private final MovieAwardMapper movieAwardMapper;
    private final MovieRelatedMapper movieRelatedMapper;
    private final MovieCommentMapper movieCommentMapper;
    private final MovieRatingDistMapper movieRatingDistMapper;
    private final MovieGenreRankMapper movieGenreRankMapper;
    private final MovieTop250Mapper movieTop250Mapper;
    private final MoviePlayLinkMapper moviePlayLinkMapper;
    private final ImportLogMapper importLogMapper;
    private final TransactionTemplate transactionTemplate;
    private final PosterFileImporter posterFileImporter;

    @Value("${moveme.seed.json-path:classpath:/seed/top250.json}")
    private Resource jsonResource;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

    // ==================== 公共流程 ====================

    @Override
    public ImportResult importAll() {
        long t0 = System.currentTimeMillis();
        ImportLog logRow = startLog("TOP250_JSON", jsonResource.getDescription());

        List<SeedMovieDTO> movies;
        try {
            movies = loadJson();
        } catch (Exception e) {
            log.error("Failed to load JSON: {}", jsonResource, e);
            finishLog(logRow, 0, 0, 0, 0, 0, List.of("loadJson: " + e.getMessage()));
            return new ImportResult(0, 0, 0, 0, 0, System.currentTimeMillis() - t0,
                    List.of("loadJson: " + e.getMessage()));
        }

        ImportContext ctx = new ImportContext();
        int ok = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        for (SeedMovieDTO dto : movies) {
            // 在 tx 前快照 personCache —— 失败回滚时恢复，避免 cache 指向已 rollback 的 person
            Map<String, Long> cacheBefore = new HashMap<>(ctx.getPersonCache());
            int personsBefore = ctx.getPersonsUpserted();
            int commentsBefore = ctx.getCommentsImported();
            try {
                transactionTemplate.execute(status -> {
                    importOne(dto, ctx);
                    return null;
                });
                ok++;
            } catch (Exception e) {
                fail++;
                // 恢复：DB 已经 rollback，cache 也得 rollback
                ctx.getPersonCache().clear();
                ctx.getPersonCache().putAll(cacheBefore);
                ctx.setPersonsUpserted(personsBefore);
                ctx.setCommentsImported(commentsBefore);
                String err = "subject_id=" + dto.getSubjectId() + ": "
                        + e.getClass().getSimpleName() + " " + e.getMessage();
                errors.add(err);
                log.error("Import failed for {}", dto.getSubjectId(), e);
            }
        }

        try {
            int backfilled = movieRelatedMapper.backfillRelatedMovieIds();
            log.info("Backfilled movie_related.related_movie_id rows: {}", backfilled);
        } catch (Exception e) {
            log.warn("Backfill related_movie_id failed", e);
        }
        try {
            aggregatePersonStats();
        } catch (Exception e) {
            log.warn("Aggregate person stats failed", e);
        }

        long elapsed = System.currentTimeMillis() - t0;
        finishLog(logRow, movies.size(), ok, fail, ctx.getPersonsUpserted(), ctx.getCommentsImported(), errors);
        log.info("Top250 seed import done: total={} ok={} fail={} persons={} comments={} elapsed={}ms",
                movies.size(), ok, fail, ctx.getPersonsUpserted(), ctx.getCommentsImported(), elapsed);
        return new ImportResult(movies.size(), ok, fail, ctx.getPersonsUpserted(),
                ctx.getCommentsImported(), elapsed, errors);
    }

    @Override
    public void importOne(SeedMovieDTO dto, ImportContext ctx) {
        repairText(dto);
        Map<String, Long> personIdByDoubanId = upsertAllPersons(dto, ctx);
        Long movieId = upsertMovie(dto);

        Set<Integer> genreIds = ensureGenreIds(dto.getGenres());
        Set<Integer> countryIds = ensureCountryIds(dto.getCountries());
        Set<Integer> languageIds = ensureLanguageIds(dto.getLanguages());
        Set<Integer> tagIds = ensureTagIds(dto.getTags());

        replaceMovieGenre(movieId, genreIds);
        replaceMovieCountry(movieId, countryIds);
        replaceMovieLanguage(movieId, languageIds);
        replaceMovieTag(movieId, tagIds);
        replaceMovieDirectors(movieId, dto.getDirectorDetails(), personIdByDoubanId);
        replaceMovieWriters(movieId, dto.getWriterDetails(), personIdByDoubanId);
        replaceMovieActors(movieId, pickActorRefs(dto), personIdByDoubanId);
        replaceMovieAka(movieId, dto.getAka());
        replaceMovieReleaseDates(movieId, dto.getReleaseDates());

        replaceMovieAwards(movieId, dto.getAwards(), personIdByDoubanId);
        replaceMovieRelated(movieId, dto.getRelatedMovies());
        replaceMoviePlayLinks(movieId, dto.getPlayLinks());
        int newComments = replaceMovieComments(movieId, dto.getComments());
        ctx.addComments(newComments);
        replaceMovieRatingDist(movieId, dto.getRatingBreakdown());
        replaceMovieGenreRank(movieId, dto.getRatingBetterThan());

        upsertTop250(movieId, dto.getTop250Rank(), dto.getTop250ListTitle(), dto.getTop250Quote());

        try {
            posterFileImporter.copy(dto, movieId);
        } catch (Exception e) {
            log.warn("Poster copy failed for {}: {}", dto.getSubjectId(), e.getMessage());
        }

        Movie mark = new Movie();
        mark.setId(movieId);
        mark.setDetailFetchedAt(LocalDateTime.now());
        movieMapper.updateById(mark);
    }

    @Override
    public ImportResult reimport() {
        log.warn("reimport(): truncating business tables");
        // FK 用 ON DELETE CASCADE，DELETE FROM movies 即可级联清掉所有关联+富字段
        // 但 persons 不在 movies 的 cascade 链上，需要单独清。字典/users/字典/award_ceremonies 保留。
        movieMapper.delete(null);
        personMapper.delete(null);
        return importAll();
    }

    // ==================== JSON / 日志 ====================

    private List<SeedMovieDTO> loadJson() throws Exception {
        try (InputStream in = jsonResource.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<SeedMovieDTO>>() {});
        }
    }

    private ImportLog startLog(String source, String filePath) {
        ImportLog row = new ImportLog();
        row.setSource(source);
        row.setFilePath(filePath);
        row.setStartedAt(LocalDateTime.now());
        try {
            importLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("startLog failed (continuing without log row)", e);
        }
        return row;
    }

    private void finishLog(ImportLog row, int total, int ok, int fail, int persons, int comments,
                           List<String> errors) {
        if (row == null || row.getId() == null) return;
        row.setMoviesTotal(total);
        row.setMoviesOk(ok);
        row.setMoviesFail(fail);
        row.setPersonsOk(persons);
        row.setCommentsOk(comments);
        row.setFinishedAt(LocalDateTime.now());
        try {
            row.setErrors(objectMapper.writeValueAsString(errors == null ? List.of() : errors));
        } catch (Exception ignored) {
            row.setErrors(null);
        }
        try {
            importLogMapper.updateById(row);
        } catch (Exception e) {
            log.warn("finishLog failed", e);
        }
    }

    private void repairText(SeedMovieDTO dto) {
        if (dto == null) return;
        dto.setTitle(TextEncodingRepairUtil.repairIfNeeded(dto.getTitle()));
        dto.setSummary(TextEncodingRepairUtil.repairIfNeeded(dto.getSummary()));
        dto.setTop250Quote(TextEncodingRepairUtil.repairIfNeeded(dto.getTop250Quote()));
    }

    private List<SeedPersonRefDTO> pickActorRefs(SeedMovieDTO dto) {
        // 先收 celebrity_preview 里"演员/主演"角色（带 avatar + role），按 id 建索引
        Map<String, SeedPersonRefDTO> previewActors = new LinkedHashMap<>();
        if (dto.getCelebrityPreview() != null) {
            for (SeedPersonRefDTO p : dto.getCelebrityPreview()) {
                if (p == null) continue;
                String role = p.getRole();
                if (role != null && !"演员".equals(role) && !"主演".equals(role)) continue;
                if (StringUtils.hasText(p.getId())) previewActors.put(p.getId(), p);
            }
        }
        // 再走 actor_details（含全部演员的 id），合并 avatar + role
        List<SeedPersonRefDTO> result = new ArrayList<>();
        if (dto.getActorDetails() != null) {
            for (SeedPersonRefDTO d : dto.getActorDetails()) {
                if (d == null) continue;
                SeedPersonRefDTO preview = StringUtils.hasText(d.getId()) ? previewActors.get(d.getId()) : null;
                if (preview != null) {
                    if (d.getAvatar() == null) d.setAvatar(preview.getAvatar());
                    if (d.getRole() == null) d.setRole(preview.getRole());
                    if (d.getTitle() == null) d.setTitle(preview.getTitle());
                }
                result.add(d);
            }
        } else if (!previewActors.isEmpty()) {
            result.addAll(previewActors.values());
        }
        return result;
    }

    // ==================== 1. upsertMovie ====================

    private Long upsertMovie(SeedMovieDTO dto) {
        Movie m = new Movie();
        m.setDoubanId(dto.getSubjectId());
        m.setImdbId(dto.getImdb());

        m.setTitle(dto.getTitle());
        String[] titleParts = SeedTextUtils.splitTitle(dto.getTitle());
        m.setTitleCn(titleParts[0]);
        m.setTitleEn(titleParts[1]);

        m.setSummary(dto.getSummary());
        m.setSummaryShort(SeedTextUtils.truncate(dto.getSummary(), 200));

        if (dto.getYear() != null) m.setYear(dto.getYear().shortValue());

        if (dto.getRuntimes() != null && !dto.getRuntimes().isEmpty()) {
            String first = dto.getRuntimes().get(0);
            m.setDurationText(first);
            Integer mins = SeedTextUtils.extractMinutes(first);
            if (mins != null) m.setDurationMinutes(mins.shortValue());
        }

        m.setReleaseDate(earliestReleaseDate(dto.getReleaseDates()));

        m.setPosterUrl(dto.getCoverImage());
        m.setOfficialSite(dto.getOfficialSite());

        if (dto.getRating() != null) {
            if (dto.getRating().getValue() != null) {
                m.setDoubanRating(BigDecimal.valueOf(dto.getRating().getValue()));
            }
            if (dto.getRating().getVotes() != null) {
                m.setDoubanVotes(toIntSaturated(dto.getRating().getVotes()));
            }
        }
        if (dto.getInterestCounts() != null) {
            m.setWishCount(dto.getInterestCounts().getWish());
            m.setCollectCount(dto.getInterestCounts().getCollect());
        }
        m.setStatus(1);

        Movie existing = movieMapper.selectOne(
                new QueryWrapper<Movie>().eq("douban_id", dto.getSubjectId()).last("LIMIT 1"));
        if (existing == null) {
            movieMapper.insert(m);
            return m.getId();
        } else {
            m.setId(existing.getId());
            m.setCreatedAt(existing.getCreatedAt());
            // local_rating / local_votes / *_score 由站内计算 + 离线 job 维护，导入时不覆盖
            m.setLocalRating(existing.getLocalRating());
            m.setLocalVotes(existing.getLocalVotes());
            m.setPopularityScore(existing.getPopularityScore());
            m.setQualityScore(existing.getQualityScore());
            m.setFreshnessScore(existing.getFreshnessScore());
            m.setPosterLocalPath(existing.getPosterLocalPath());   // 海报由 PosterFileImporter 写
            movieMapper.updateById(m);
            return existing.getId();
        }
    }

    /** 从 release_dates 里取最早的 LocalDate；解析失败的整体跳过。 */
    private LocalDate earliestReleaseDate(List<String> rawList) {
        if (rawList == null) return null;
        return rawList.stream()
                .map(SeedTextUtils::parseRelease)
                .map(SeedTextUtils.ReleaseParse::date)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private Integer toIntSaturated(Long votes) {
        if (votes == null) return null;
        if (votes > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (votes < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return votes.intValue();
    }

    // ==================== 2. upsertAllPersons ====================

    private Map<String, Long> upsertAllPersons(SeedMovieDTO dto, ImportContext ctx) {
        Map<String, SeedPersonRefDTO> refs = new LinkedHashMap<>();
        addRefs(refs, dto.getDirectorDetails());
        addRefs(refs, dto.getWriterDetails());
        addRefs(refs, dto.getActorDetails());
        addRefs(refs, dto.getCelebrityPreview());
        if (dto.getAwards() != null) {
            for (SeedAwardDTO a : dto.getAwards()) {
                if (a != null) addRefs(refs, a.getRecipients());
            }
        }

        Map<String, Long> result = new HashMap<>();
        for (SeedPersonRefDTO ref : refs.values()) {
            String key = personKey(ref);
            Long cached = ctx.getCachedPersonId(key);
            if (cached != null) {
                result.put(key, cached);
                continue;
            }
            Person p = personMapper.selectOne(
                    new QueryWrapper<Person>().eq("douban_person_id", key).last("LIMIT 1"));
            if (p == null) {
                p = new Person();
                p.setDoubanPersonId(key);
                p.setName(ref.getName());
                p.setNameEn(extractNameEn(ref));
                p.setAvatarUrl(ref.getAvatar());
                p.setProfileUrl(ref.getUrl());
                personMapper.insert(p);
                ctx.incrementPersons();
            } else {
                boolean dirty = false;
                if (p.getAvatarUrl() == null && ref.getAvatar() != null) {
                    p.setAvatarUrl(ref.getAvatar());
                    dirty = true;
                }
                String nameEn = extractNameEn(ref);
                if (p.getNameEn() == null && nameEn != null) {
                    p.setNameEn(nameEn);
                    dirty = true;
                }
                if (p.getProfileUrl() == null && ref.getUrl() != null) {
                    p.setProfileUrl(ref.getUrl());
                    dirty = true;
                }
                if (dirty) personMapper.updateById(p);
            }
            ctx.cachePersonId(key, p.getId());
            result.put(key, p.getId());
        }
        return result;
    }

    private void addRefs(Map<String, SeedPersonRefDTO> map, List<SeedPersonRefDTO> list) {
        if (list == null) return;
        for (SeedPersonRefDTO r : list) {
            if (r == null) continue;
            String k = personKey(r);
            SeedPersonRefDTO old = map.get(k);
            if (old == null) {
                map.put(k, r);
                continue;
            }
            // 已存在 → merge：补 avatar / title / role
            if (old.getAvatar() == null && r.getAvatar() != null) old.setAvatar(r.getAvatar());
            if (old.getTitle() == null && r.getTitle() != null) old.setTitle(r.getTitle());
            if (old.getRole() == null && r.getRole() != null) old.setRole(r.getRole());
        }
    }

    private String personKey(SeedPersonRefDTO ref) {
        if (StringUtils.hasText(ref.getId())) return ref.getId();
        // 合成 key 必须 ≤ 20 字符（schema 列宽）。用 hash 缩短：
        // 'syn_' + 16 hex of SHA1(name|title) = 20 字符
        String raw = safe(ref.getName()) + "|" + safe(ref.getTitle());
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-1")
                    .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("syn_");
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();   // 4 + 16 = 20
        } catch (java.security.NoSuchAlgorithmException e) {
            return "syn_" + Math.abs(raw.hashCode());
        }
    }

    private String extractNameEn(SeedPersonRefDTO ref) {
        String t = ref.getTitle();
        if (!StringUtils.hasText(t)) return null;
        int sp = t.indexOf(' ');
        if (sp <= 0) return null;
        String tail = t.substring(sp + 1).trim();
        return SeedTextUtils.containsLatin(tail) ? tail : null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // ==================== 4. ensure*Ids（字典懒插）====================

    private Set<Integer> ensureGenreIds(List<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        Set<Integer> ids = new LinkedHashSet<>();
        for (String raw : names) {
            if (!StringUtils.hasText(raw)) continue;
            String name = raw.trim();
            Genre g = genreMapper.selectOne(
                    new QueryWrapper<Genre>().eq("name", name).last("LIMIT 1"));
            if (g == null) {
                g = new Genre();
                g.setName(name);
                genreMapper.insert(g);
            }
            ids.add(g.getId());
        }
        return ids;
    }

    private Set<Integer> ensureCountryIds(List<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        Set<Integer> ids = new LinkedHashSet<>();
        for (String raw : names) {
            if (!StringUtils.hasText(raw)) continue;
            String name = raw.trim();
            Country c = countryMapper.selectOne(
                    new QueryWrapper<Country>().eq("name", name).last("LIMIT 1"));
            if (c == null) {
                c = new Country();
                c.setName(name);
                countryMapper.insert(c);
            }
            ids.add(c.getId());
        }
        return ids;
    }

    private Set<Integer> ensureLanguageIds(List<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        Set<Integer> ids = new LinkedHashSet<>();
        for (String raw : names) {
            if (!StringUtils.hasText(raw)) continue;
            String name = raw.trim();
            Language l = languageMapper.selectOne(
                    new QueryWrapper<Language>().eq("name", name).last("LIMIT 1"));
            if (l == null) {
                l = new Language();
                l.setName(name);
                languageMapper.insert(l);
            }
            ids.add(l.getId());
        }
        return ids;
    }

    private Set<Integer> ensureTagIds(List<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        Set<Integer> ids = new LinkedHashSet<>();
        for (String raw : names) {
            if (!StringUtils.hasText(raw)) continue;
            String name = raw.trim();
            Tag t = tagMapper.selectOne(
                    new QueryWrapper<Tag>().eq("name", name).last("LIMIT 1"));
            if (t == null) {
                t = new Tag();
                t.setName(name);
                t.setUsageCount(0);
                tagMapper.insert(t);
            }
            ids.add(t.getId());
        }
        return ids;
    }

    // ==================== 5. replaceMovie{Genre,Country,Language,Tag} ====================

    private void replaceMovieGenre(Long movieId, Set<Integer> genreIds) {
        movieGenreMapper.delete(new QueryWrapper<MovieGenre>().eq("movie_id", movieId));
        for (Integer gid : genreIds) {
            MovieGenre row = new MovieGenre();
            row.setMovieId(movieId);
            row.setGenreId(gid);
            movieGenreMapper.insert(row);
        }
    }

    private void replaceMovieCountry(Long movieId, Set<Integer> countryIds) {
        movieCountryMapper.delete(new QueryWrapper<MovieCountry>().eq("movie_id", movieId));
        for (Integer cid : countryIds) {
            MovieCountry row = new MovieCountry();
            row.setMovieId(movieId);
            row.setCountryId(cid);
            movieCountryMapper.insert(row);
        }
    }

    private void replaceMovieLanguage(Long movieId, Set<Integer> languageIds) {
        movieLanguageMapper.delete(new QueryWrapper<MovieLanguage>().eq("movie_id", movieId));
        for (Integer lid : languageIds) {
            MovieLanguage row = new MovieLanguage();
            row.setMovieId(movieId);
            row.setLanguageId(lid);
            movieLanguageMapper.insert(row);
        }
    }

    private void replaceMovieTag(Long movieId, Set<Integer> tagIds) {
        movieTagMapper.delete(new QueryWrapper<MovieTag>().eq("movie_id", movieId));
        for (Integer tid : tagIds) {
            MovieTag row = new MovieTag();
            row.setMovieId(movieId);
            row.setTagId(tid);
            movieTagMapper.insert(row);
        }
    }

    // ==================== 6. replaceMovie{Directors,Writers,Actors} ====================

    private void replaceMovieDirectors(Long movieId, List<SeedPersonRefDTO> details,
                                       Map<String, Long> personIdByKey) {
        movieDirectorMapper.delete(new QueryWrapper<MovieDirector>().eq("movie_id", movieId));
        if (details == null) return;
        int order = 0;
        for (SeedPersonRefDTO ref : details) {
            if (ref == null) continue;
            Long pid = personIdByKey.get(personKey(ref));
            if (pid == null) continue;
            order++;
            MovieDirector row = new MovieDirector();
            row.setMovieId(movieId);
            row.setPersonId(pid);
            row.setSortOrder(order);
            try {
                movieDirectorMapper.insert(row);
            } catch (DuplicateKeyException ignored) {
                // 同一部电影同一人重复出现（罕见数据）— 跳过
            }
        }
    }

    private void replaceMovieWriters(Long movieId, List<SeedPersonRefDTO> details,
                                     Map<String, Long> personIdByKey) {
        movieWriterMapper.delete(new QueryWrapper<MovieWriter>().eq("movie_id", movieId));
        if (details == null) return;
        int order = 0;
        for (SeedPersonRefDTO ref : details) {
            if (ref == null) continue;
            Long pid = personIdByKey.get(personKey(ref));
            if (pid == null) continue;
            order++;
            MovieWriter row = new MovieWriter();
            row.setMovieId(movieId);
            row.setPersonId(pid);
            row.setSortOrder(order);
            try {
                movieWriterMapper.insert(row);
            } catch (DuplicateKeyException ignored) {
                // 同人重复 — 跳过
            }
        }
    }

    private void replaceMovieActors(Long movieId, List<SeedPersonRefDTO> refs,
                                    Map<String, Long> personIdByKey) {
        movieActorMapper.delete(new QueryWrapper<MovieActor>().eq("movie_id", movieId));
        if (refs == null) return;
        int order = 0;
        Set<Long> seen = new java.util.HashSet<>();
        for (SeedPersonRefDTO ref : refs) {
            if (ref == null) continue;
            Long pid = personIdByKey.get(personKey(ref));
            if (pid == null || !seen.add(pid)) continue;   // 防同部同人重复
            order++;
            MovieActor row = new MovieActor();
            row.setMovieId(movieId);
            row.setPersonId(pid);
            row.setRoleName(ref.getRole());
            row.setSortOrder(order);
            row.setIsLead(order <= 5 ? 1 : 0);
            movieActorMapper.insert(row);
        }
    }

    // ==================== 7-8. aka / release_dates ====================

    private void replaceMovieAka(Long movieId, List<String> akaList) {
        movieAkaMapper.delete(new QueryWrapper<MovieAka>().eq("movie_id", movieId));
        if (akaList == null) return;
        for (String t : akaList) {
            if (!StringUtils.hasText(t)) continue;
            MovieAka row = new MovieAka();
            row.setMovieId(movieId);
            row.setTitle(t.trim());
            movieAkaMapper.insert(row);
        }
    }

    private void replaceMovieReleaseDates(Long movieId, List<String> rawList) {
        movieReleaseDateMapper.delete(
                new QueryWrapper<MovieReleaseDate>().eq("movie_id", movieId));
        if (rawList == null) return;
        for (String raw : rawList) {
            if (!StringUtils.hasText(raw)) continue;
            MovieReleaseDate row = new MovieReleaseDate();
            row.setMovieId(movieId);
            row.setRawText(raw.trim());
            SeedTextUtils.ReleaseParse parsed = SeedTextUtils.parseRelease(raw);
            row.setReleaseAt(parsed.date());
            row.setRegion(parsed.region());
            movieReleaseDateMapper.insert(row);
        }
    }

    // ==================== 9. awards ====================

    private void replaceMovieAwards(Long movieId, List<SeedAwardDTO> awards,
                                    Map<String, Long> personIdByKey) {
        movieAwardMapper.delete(new QueryWrapper<MovieAward>().eq("movie_id", movieId));
        if (awards == null) return;
        for (SeedAwardDTO a : awards) {
            if (a == null || !StringUtils.hasText(a.getName())) continue;
            MovieAward row = new MovieAward();
            row.setMovieId(movieId);
            row.setCeremonyId(matchCeremonyId(a.getName()));
            row.setCeremonyText(a.getName());
            row.setCategory(a.getCategory());
            row.setStatus(SeedTextUtils.normalizeAwardStatus(a.getStatus()));
            row.setAwardUrl(a.getUrl());

            if (a.getRecipients() != null && !a.getRecipients().isEmpty()) {
                SeedPersonRefDTO first = a.getRecipients().get(0);
                row.setRecipientPersonId(personIdByKey.get(personKey(first)));
                row.setRecipientText(a.getRecipients().stream()
                        .map(SeedPersonRefDTO::getName)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(", ")));
            }
            movieAwardMapper.insert(row);
        }
    }

    /** 在 award_ceremonies 里找哪条 name 是 ceremonyText 的子串。匹配不到返回 null。 */
    private Integer matchCeremonyId(String ceremonyText) {
        if (!StringUtils.hasText(ceremonyText)) return null;
        // award_ceremonies 只有 ~12 条，全捞下来内存匹配最稳
        for (AwardCeremony c : awardCeremonyMapper.selectList(null)) {
            if (c.getName() != null && ceremonyText.contains(c.getName())) {
                return c.getId();
            }
        }
        return null;
    }

    // ==================== 10. related ====================

    private void replaceMovieRelated(Long movieId, List<SeedRelatedMovieDTO> related) {
        movieRelatedMapper.delete(new QueryWrapper<MovieRelated>().eq("movie_id", movieId));
        if (related == null) return;
        int order = 0;
        for (SeedRelatedMovieDTO r : related) {
            if (r == null) continue;
            order++;
            MovieRelated row = new MovieRelated();
            row.setMovieId(movieId);
            row.setRelatedDoubanId(r.getSubjectId());
            row.setRelatedTitle(r.getTitle());
            if (r.getRating() != null) row.setRelatedRating(BigDecimal.valueOf(r.getRating()));
            row.setRelatedCoverUrl(r.getCoverImage());
            row.setSortOrder(order);
            // related_movie_id 留 NULL，全量后 backfill
            movieRelatedMapper.insert(row);
        }
    }

    // ==================== 10b. play_links ====================

    private void replaceMoviePlayLinks(Long movieId, List<SeedPlayLinkDTO> playLinks) {
        moviePlayLinkMapper.delete(new QueryWrapper<MoviePlayLink>().eq("movie_id", movieId));
        if (playLinks == null) return;
        for (SeedPlayLinkDTO pl : playLinks) {
            if (pl == null || !StringUtils.hasText(pl.getUrl())) continue;
            MoviePlayLink row = new MoviePlayLink();
            row.setMovieId(movieId);
            row.setPlatform(pl.getName());
            row.setUrl(pl.getUrl());
            moviePlayLinkMapper.insert(row);
        }
    }

    // ==================== 11. comments ====================

    private int replaceMovieComments(Long movieId, List<SeedCommentDTO> comments) {
        if (comments == null) return 0;
        int inserted = 0;
        for (SeedCommentDTO c : comments) {
            if (c == null || !StringUtils.hasText(c.getContent())) continue;

            // 防重：已存在的 douban_comment_id 直接跳过（不更新，因为豆瓣评论是不可变的）
            if (StringUtils.hasText(c.getCommentId())) {
                Long cnt = movieCommentMapper.selectCount(
                        new QueryWrapper<MovieComment>().eq("douban_comment_id", c.getCommentId()));
                if (cnt > 0) continue;
            }

            MovieComment row = new MovieComment();
            row.setMovieId(movieId);
            row.setSource(0);   // 0 = 豆瓣源
            row.setDoubanCommentId(c.getCommentId());
            if (c.getUser() != null) {
                row.setAuthorName(c.getUser().getName());
                row.setAuthorAvatar(c.getUser().getAvatar());
            }
            row.setAuthorLocation(c.getLocation());
            if (c.getRating() != null) {
                if (c.getRating().getValue() != null) {
                    row.setRating(c.getRating().getValue().intValue());
                }
                row.setRatingLabel(c.getRating().getLabel());
            }
            row.setContent(c.getContent());
            row.setVotes(c.getVotes());
            row.setPostedAt(c.getCreatedAt());
            row.setSourceUrl(c.getSourceUrl());
            try {
                movieCommentMapper.insert(row);
                inserted++;
            } catch (DuplicateKeyException ignored) {
                // 极少：JSON 内同 commentId 出现两次
            }
        }
        return inserted;
    }

    // ==================== 12. rating_dist / genre_rank ====================

    private void replaceMovieRatingDist(Long movieId, List<SeedRatingBreakdownDTO> dist) {
        movieRatingDistMapper.delete(
                new QueryWrapper<MovieRatingDist>().eq("movie_id", movieId));
        if (dist == null) return;
        for (SeedRatingBreakdownDTO d : dist) {
            if (d == null || d.getStar() == null) continue;
            MovieRatingDist row = new MovieRatingDist();
            row.setMovieId(movieId);
            row.setStar(d.getStar());
            row.setLabel(StringUtils.hasText(d.getText()) ? d.getText() : d.getLabel());
            row.setPercentage(d.getPercentage() == null ? BigDecimal.ZERO : d.getPercentage());
            movieRatingDistMapper.insert(row);
        }
    }

    private void replaceMovieGenreRank(Long movieId, List<SeedRatingBetterThanDTO> ranks) {
        movieGenreRankMapper.delete(
                new QueryWrapper<MovieGenreRank>().eq("movie_id", movieId));
        if (ranks == null) return;
        Set<String> seen = new java.util.HashSet<>();
        for (SeedRatingBetterThanDTO r : ranks) {
            if (r == null || !StringUtils.hasText(r.getGenre())) continue;
            // 联合主键 (movie_id, genre_name) 防重
            if (!seen.add(r.getGenre())) continue;
            MovieGenreRank row = new MovieGenreRank();
            row.setMovieId(movieId);
            row.setGenreName(r.getGenre());
            row.setPercentile(r.getPercentage() == null ? BigDecimal.ZERO : r.getPercentage());
            row.setRankUrl(r.getUrl());
            movieGenreRankMapper.insert(row);
        }
    }

    // ==================== 13. top250 ====================

    private void upsertTop250(Long movieId, Short rankNo, String listTitle, String quote) {
        if (rankNo == null) return;
        MovieTop250 existing = movieTop250Mapper.selectById(movieId);
        MovieTop250 row = existing == null ? new MovieTop250() : existing;
        row.setMovieId(movieId);
        row.setRankNo(rankNo);
        row.setListTitle(listTitle);
        row.setQuote(quote);
        row.setSnapshotAt(LocalDateTime.now());
        if (existing == null) movieTop250Mapper.insert(row);
        else movieTop250Mapper.updateById(row);
    }

    // ==================== 14. aggregate person stats ====================

    /**
     * 全量后聚合 persons.movie_count / avg_movie_rating。
     * 250 部规模直接 SQL 跑，毫秒级。
     */
    private void aggregatePersonStats() {
        personMapper.recalcMovieCount();
        personMapper.recalcAvgMovieRating();
    }
}
