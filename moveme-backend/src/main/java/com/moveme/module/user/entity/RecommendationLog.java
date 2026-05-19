package com.moveme.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommendation_logs")
public class RecommendationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String strategyType;
    private String requestData;
    private String responseData;
    private String llmProvider;
    private Integer latencyMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
