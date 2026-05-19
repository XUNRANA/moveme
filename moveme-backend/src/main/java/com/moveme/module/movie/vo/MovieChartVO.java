package com.moveme.module.movie.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MovieChartVO {

    private String genreName;
    private String boardTitle;
    private List<ChartMovieItem> movies;

    @Data
    public static class ChartMovieItem {
        private Long movieId;
        private String title;
        private String posterUrl;
        private BigDecimal rating;
        private Short year;
        private Integer rankNo;
    }
}
