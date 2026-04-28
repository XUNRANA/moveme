package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_director")
public class MovieDirector {

    private Long movieId;
    private Long personId;
    private Integer sortOrder;
}
