package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("movie_comment")
public class MovieComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;
    /** 0=豆瓣抓取, 1=站内用户 */
    private Integer source;
    private String doubanCommentId;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String authorLocation;
    /** 1-5 星 */
    private Integer rating;
    private String ratingLabel;
    private String content;
    private Integer votes;
    private LocalDateTime postedAt;
    private String sourceUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
