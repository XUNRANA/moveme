package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("movie_genre_rank")
public class MovieGenreRank {

    private Long movieId;
    private String genreName;
    private BigDecimal percentile;
    private String rankUrl;
}
