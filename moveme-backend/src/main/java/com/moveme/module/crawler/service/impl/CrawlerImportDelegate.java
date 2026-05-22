package com.moveme.module.crawler.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moveme.module.movie.mapper.MovieRelatedMapper;
import com.moveme.module.movie.mapper.PersonMapper;
import com.moveme.module.seed.dto.SeedMovieDTO;
import com.moveme.module.seed.service.SeedImportService;
import com.moveme.module.seed.support.ImportContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerImportDelegate {

    private final SeedImportService seedImportService;
    private final MovieRelatedMapper movieRelatedMapper;
    private final PersonMapper personMapper;
    private final TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

    public ImportStats importJson(Path jsonPath) {
        if (!jsonPath.toFile().exists()) {
            log.warn("JSON file not found: {}", jsonPath);
            return new ImportStats(0, 0, 0, 0, List.of("file not found: " + jsonPath));
        }

        List<SeedMovieDTO> movies;
        try (InputStream in = new FileInputStream(jsonPath.toFile())) {
            movies = objectMapper.readValue(in, new TypeReference<List<SeedMovieDTO>>() {});
        } catch (Exception e) {
            log.error("Failed to read JSON: {}", jsonPath, e);
            return new ImportStats(0, 0, 0, 0, List.of("read json: " + e.getMessage()));
        }

        if (movies == null || movies.isEmpty()) {
            log.warn("Empty movie list in JSON: {}", jsonPath);
            return new ImportStats(0, 0, 0, 0, List.of());
        }

        ImportContext ctx = new ImportContext();
        int ok = 0, fail = 0, persons = 0, comments = 0;
        List<String> errors = new ArrayList<>();

        for (SeedMovieDTO dto : movies) {
            if (dto == null || dto.getSubjectId() == null) continue;
            Map<String, Long> cacheBefore = new HashMap<>(ctx.getPersonCache());
            int personsBefore = ctx.getPersonsUpserted();
            int commentsBefore = ctx.getCommentsImported();
            try {
                transactionTemplate.execute(status -> {
                    seedImportService.importOne(dto, ctx);
                    return null;
                });
                ok++;
            } catch (Exception e) {
                fail++;
                ctx.getPersonCache().clear();
                ctx.getPersonCache().putAll(cacheBefore);
                ctx.setPersonsUpserted(personsBefore);
                ctx.setCommentsImported(commentsBefore);
                String err = dto.getSubjectId() + ": " + e.getClass().getSimpleName() + " " + e.getMessage();
                errors.add(err);
                log.error("Import failed: {}", dto.getSubjectId(), e);
            }
        }

        persons = ctx.getPersonsUpserted();
        comments = ctx.getCommentsImported();

        try {
            int backfilled = movieRelatedMapper.backfillRelatedMovieIds();
            log.info("Backfilled related_movie_id rows: {}", backfilled);
        } catch (Exception e) {
            log.warn("Backfill related_movie_id failed", e);
        }
        try {
            personMapper.recalcMovieCount();
            personMapper.recalcAvgMovieRating();
        } catch (Exception e) {
            log.warn("Aggregate person stats failed", e);
        }

        log.info("Crawler import done: total={} ok={} fail={} persons={} comments={}",
                movies.size(), ok, fail, persons, comments);
        return new ImportStats(movies.size(), ok, fail, comments, errors);
    }

    public record ImportStats(int total, int ok, int fail, int comments, List<String> errors) {}
}
