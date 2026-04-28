package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("movies")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String doubanId;
    private String imdbId;

    private String title;
    private String titleCn;
    private String titleEn;
    private String titlePinyin;

    private String summary;
    private String summaryShort;

    private Short year;
    private Short durationMinutes;
    private String durationText;
    private LocalDate releaseDate;

    private String posterUrl;
    private String posterLocalPath;
    private String backdropUrl;
    private String trailerUrl;
    private String officialSite;

    private BigDecimal doubanRating;
    private Integer doubanVotes;
    private BigDecimal localRating;
    private Integer localVotes;

    private Integer wishCount;
    private Integer collectCount;

    private BigDecimal popularityScore;
    private BigDecimal qualityScore;
    private BigDecimal freshnessScore;

    private LocalDateTime detailFetchedAt;
    private LocalDateTime lastCrawledAt;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
