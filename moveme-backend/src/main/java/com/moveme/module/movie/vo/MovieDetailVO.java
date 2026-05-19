package com.moveme.module.movie.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MovieDetailVO extends MovieVO {

    private LocalDate releaseDate;
    private String imdbId;
    private List<String> countries;
    private List<String> languages;
    private List<PersonBriefVO> directors;
    private List<PersonBriefVO> actors;
    private List<PersonBriefVO> writers;

    // Phase B enriched fields
    private List<String> akas;
    private List<String> tags;
    private List<ReleaseDateVO> releaseDates;
    private List<AwardVO> awards;
    private List<RelatedMovieVO> relatedMovies;
    private List<RatingDistVO> ratingDist;
    private List<GenreRankVO> genreRanks;
    private Top250VO top250;
    private List<PlayLinkVO> playLinks;

    @Data
    public static class PlayLinkVO {
        private String platform;
        private String url;
    }

    @Data
    public static class ReleaseDateVO {
        private LocalDate date;
        private String region;
        private String rawText;
    }

    @Data
    public static class AwardVO {
        private String ceremony;
        private String category;
        private String status;
        private String recipient;
        private String url;
    }

    @Data
    public static class CommentVO {
        private Long id;
        private String authorName;
        private String authorAvatar;
        private String authorLocation;
        private Integer rating;
        private String ratingLabel;
        private String content;
        private Integer votes;
        private String postedAt;
        private String sourceUrl;
        private Boolean liked;
    }

    @Data
    public static class RelatedMovieVO {
        private Long movieId;
        private String title;
        private BigDecimal rating;
        private String coverUrl;
    }

    @Data
    public static class RatingDistVO {
        private Integer star;
        private String label;
        private BigDecimal percentage;
    }

    @Data
    public static class GenreRankVO {
        private String genre;
        private BigDecimal percentile;
    }

    @Data
    public static class Top250VO {
        private Integer rank;
        private String listTitle;
        private String quote;
    }
}
