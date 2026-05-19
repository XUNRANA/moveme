package com.moveme.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("view_history")
public class ViewHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long movieId;
    private LocalDateTime viewedAt;
}
