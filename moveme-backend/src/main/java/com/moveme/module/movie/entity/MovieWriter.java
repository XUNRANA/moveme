package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_writer")
public class MovieWriter {

    private Long movieId;
    private Long personId;
    private Integer sortOrder;
}
