package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("movie_release_date")
public class MovieReleaseDate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    private LocalDate releaseAt;
    private String region;
    private String rawText;
}
