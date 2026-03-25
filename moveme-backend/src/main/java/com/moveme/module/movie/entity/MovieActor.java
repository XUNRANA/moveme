package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_actors")
public class MovieActor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    private String name;
    private String roleName;
    private Integer sortOrder;
}
