package com.moveme.module.movie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PersonDetailVO {

    private Long id;
    private String name;
    private String nameEn;
    private String avatarUrl;
    private String avatarLocalPath;
    private String gender;        // "男" / "女" / "未知"
    private String birthDate;     // "1974-11-11"
    private String birthPlace;
    private String bio;
    private Integer movieCount;
    private BigDecimal avgMovieRating;

    private List<FilmographyItem> directed;
    private List<FilmographyItem> written;
    private List<FilmographyItem> acted;

    @Data
    public static class FilmographyItem {
        private Long movieId;
        private String title;
        private String posterUrl;
        private BigDecimal rating;
        private Integer year;
        private String roleName;
    }
}
