package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_genre")
public class MovieGenre {

    private Long movieId;
    private Integer genreId;
}
