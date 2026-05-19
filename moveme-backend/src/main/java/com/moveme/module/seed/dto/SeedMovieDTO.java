package com.moveme.module.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 顶层种子电影 DTO，1:1 贴合 data/top250.json。
 *
 * 字段全部使用 camelCase；ObjectMapper 配置 PropertyNamingStrategies.SNAKE_CASE
 * 自动把 JSON 的 snake_case 映射过来。
 *
 * 标 @JsonIgnoreProperties(ignoreUnknown = true) 忽略未来豆瓣 schema 变化新增的字段。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeedMovieDTO {

    private String subjectId;
    private String url;
    /** 主标题，"肖申克的救赎 The Shawshank Redemption" */
    private String title;
    private Integer year;
    private String coverImage;

    private SeedRatingDTO rating;

    /** 仅名字字符串列表（供降级 fallback；优先用 *_details / celebrity_preview） */
    private List<String> directors;
    private List<String> writers;
    private List<String> actors;

    private List<String> genres;
    private List<String> countries;
    private List<String> languages;
    /** "1994-09-10(多伦多电影节)" / "1994-10-14(美国)" */
    private List<String> releaseDates;
    /** "142分钟" */
    private List<String> runtimes;
    /** "月黑高飞(港)" / "刺激1995(台)" */
    private List<String> aka;

    private String officialSite;
    private String imdb;
    private String summary;
    private List<String> tags;

    private Short top250Rank;
    private String top250ListTitle;
    private List<String> top250OtherTitles;
    private String top250Quote;

    private List<SeedRatingBreakdownDTO> ratingBreakdown;
    private List<SeedRatingBetterThanDTO> ratingBetterThan;
    private List<SeedRelatedMovieDTO> relatedMovies;
    private List<SeedPersonRefDTO> celebrityPreview;
    private List<SeedAwardDTO> awards;

    private List<SeedPersonRefDTO> directorDetails;
    private List<SeedPersonRefDTO> writerDetails;
    private List<SeedPersonRefDTO> actorDetails;

    private SeedInterestCountsDTO interestCounts;
    private List<SeedCommentDTO> comments;
    private Integer commentsCount;

    private List<SeedPlayLinkDTO> playLinks;
}
