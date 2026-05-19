package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_tag")
public class MovieTag {

    private Long movieId;
    private Integer tagId;
}
