package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_play_link")
public class MoviePlayLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    private String platform;
    private String url;
}
