package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("movie_top250")
public class MovieTop250 {

    /** PK 就是 movie_id（1:1 关系，复用 movies.id） */
    @TableId(type = IdType.INPUT)
    private Long movieId;

    private Short rankNo;
    private String listTitle;
    private String quote;
    private LocalDateTime snapshotAt;
}
