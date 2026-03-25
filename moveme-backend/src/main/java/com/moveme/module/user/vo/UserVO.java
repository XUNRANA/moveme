package com.moveme.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer role;
    private LocalDateTime createdAt;
}
