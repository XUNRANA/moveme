package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("movie_rating_dist")
public class MovieRatingDist {

    private Long movieId;
    /** 1-5 */
    private Integer star;
    private String label;
    private BigDecimal percentage;
}
