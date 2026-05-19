package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("movie_related")
public class MovieRelated {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    private Long relatedMovieId;
    private String relatedDoubanId;
    private String relatedTitle;
    private BigDecimal relatedRating;
    private String relatedCoverUrl;
    private Integer sortOrder;
}
