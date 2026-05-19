package com.moveme.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserHistoryVO {
    private Long id;
    private Long movieId;
    private String title;
    private String posterUrl;
    private String posterLocalPath;
    private LocalDateTime viewedAt;
}
