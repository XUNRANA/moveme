package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("movie_award")
public class MovieAward {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    private Integer ceremonyId;
    private String ceremonyText;
    private String category;
    /** ENUM('won','nominated','unknown') */
    private String status;
    private String awardUrl;
    private Long recipientPersonId;
    private String recipientText;
}
