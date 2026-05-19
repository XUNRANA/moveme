package com.moveme.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ratings")
public class Rating {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long movieId;
    private Integer score;
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
