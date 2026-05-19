package com.moveme.module.seed.util;

import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.seed.dto.SeedMovieDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 把 data/images/{rank}_{title}.jpg 拷到 static/posters/{douban_id}.jpg，
 * 并把 /static/posters/{douban_id}.jpg 写入 movies.poster_local_path。
 *
 * <p>data/images 文件名形如 "001_肖申克的救赎.jpg" / "002_霸王别姬.jpg"。
 * 通过 top250_rank 数字前缀匹配，文件名匹配宽松（前缀 zero-padded）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PosterFileImporter {

    private final MovieMapper movieMapper;

    @Value("${moveme.seed.images-dir:./data/images/}")
    private String imagesDir;

    @Value("${moveme.seed.poster-out-dir:./data/posters/}")
    private String posterOutDir;

    @Value("${moveme.poster.url-prefix:/static/posters}")
    private String urlPrefix;

    /**
     * 找匹配文件并拷贝。匹配优先级：
     * 1) 文件名前缀 = 3 位数字 rank（"001_xxx.jpg"）
     * 2) 文件名包含 title 子串
     * 拷贝后写 movies.poster_local_path。
     *
     * 失败不抛异常，只 warn —— 海报导入失败不应让整部电影回滚。
     */
    public void copy(SeedMovieDTO dto, Long movieId) {
        if (dto == null || movieId == null) return;
        Path source = locateSource(dto);
        if (source == null) {
            return;   // 没找到本地文件就直接跳过（用户可能没下载 images）
        }
        Path target = Paths.get(posterOutDir, dto.getSubjectId() + ".jpg");
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Poster copy {} → {} failed: {}", source, target, e.getMessage());
            return;
        }
        String publicPath = (urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/") + dto.getSubjectId() + ".jpg";
        try {
            movieMapper.updatePosterLocalPath(movieId, publicPath);
        } catch (Exception e) {
            log.warn("Update poster_local_path for movie={} failed: {}", movieId, e.getMessage());
        }
    }

    private Path locateSource(SeedMovieDTO dto) {
        Path dir = Paths.get(imagesDir);
        if (!Files.isDirectory(dir)) return null;

        String rankPrefix = dto.getTop250Rank() != null
                ? String.format("%03d_", dto.getTop250Rank())
                : null;

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream.filter(Files::isRegularFile).toList();

            if (rankPrefix != null) {
                Optional<Path> byRank = files.stream()
                        .filter(p -> p.getFileName().toString().startsWith(rankPrefix))
                        .findFirst();
                if (byRank.isPresent()) return byRank.get();
            }

            String title = dto.getTitle();
            if (title != null && !title.isBlank()) {
                String key = title.split(" ")[0];   // "肖申克的救赎"
                Optional<Path> byTitle = files.stream()
                        .filter(p -> p.getFileName().toString().contains(key))
                        .findFirst();
                if (byTitle.isPresent()) return byTitle.get();
            }
        } catch (IOException e) {
            log.warn("List {} failed: {}", dir, e.getMessage());
        }
        return null;
    }
}
