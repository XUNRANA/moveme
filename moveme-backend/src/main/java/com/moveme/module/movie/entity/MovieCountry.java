package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_country")
public class MovieCountry {

    private Long movieId;
    private Integer countryId;
}
