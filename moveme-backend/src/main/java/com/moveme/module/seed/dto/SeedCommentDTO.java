package com.moveme.module.seed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeedCommentDTO {
    private String commentId;
    private Integer votes;
    private SeedCommentUserDTO user;
    /** 嵌套对象 {value, label}：1-5 + "力荐"/"推荐"/... */
    private SeedCommentRatingDTO rating;
    /** "看过" 等用户状态。本项目不存 movie_comment（meaning 不同于 rating label） */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private String location;
    private String content;
    private String sourceUrl;
}
