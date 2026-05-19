package com.moveme.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRatingVO {
    private Long id;
    private Long movieId;
    private String title;
    private String posterUrl;
    private String posterLocalPath;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
