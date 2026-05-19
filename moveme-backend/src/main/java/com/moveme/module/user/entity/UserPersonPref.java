package com.moveme.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_person_pref")
public class UserPersonPref {

    private Long userId;
    private Long personId;
    private String roleKind;
    private BigDecimal score;
    private LocalDateTime updatedAt;
}
