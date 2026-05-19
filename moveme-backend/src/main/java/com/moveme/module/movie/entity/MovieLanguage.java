package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_language")
public class MovieLanguage {

    private Long movieId;
    private Integer languageId;
}
