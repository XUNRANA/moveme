package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_directors")
public class MovieDirector {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    private String name;
}
