package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("movies")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String doubanId;
    private String title;
    private String originalTitle;
    private String posterUrl;
    private Short year;
    private BigDecimal doubanRating;
    private Integer doubanVotes;
    private BigDecimal localRating;
    private Integer localVotes;
    private String summary;
    private String country;
    private String language;
    private String duration;
    private LocalDate releaseDate;
    private String imdbId;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
