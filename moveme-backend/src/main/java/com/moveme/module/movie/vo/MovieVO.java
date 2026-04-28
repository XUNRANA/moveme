package com.moveme.module.movie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MovieVO {

    private Long id;
    private String doubanId;
    private String title;
    private String titleCn;
    private String titleEn;
    private String posterUrl;
    private String posterLocalPath;
    private Short year;
    private BigDecimal doubanRating;
    private Integer doubanVotes;
    private BigDecimal localRating;
    private Integer localVotes;
    private Integer wishCount;
    private Integer collectCount;
    private BigDecimal popularityScore;
    private String summary;
    private String summaryShort;
    private String durationText;
    private List<String> genres;
}
