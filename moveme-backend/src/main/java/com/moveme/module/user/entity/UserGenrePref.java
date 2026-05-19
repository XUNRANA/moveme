package com.moveme.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_genre_pref")
public class UserGenrePref {

    private Long userId;
    private Integer genreId;
    private BigDecimal score;
    private LocalDateTime updatedAt;
}
