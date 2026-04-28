package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_actor")
public class MovieActor {

    private Long movieId;
    private Long personId;
    private String roleName;
    private Integer sortOrder;
    private Integer isLead;
}
