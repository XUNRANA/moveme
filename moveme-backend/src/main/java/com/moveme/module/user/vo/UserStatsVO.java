package com.moveme.module.user.vo;

import lombok.Data;

@Data
public class UserStatsVO {
    private long ratingCount;
    private long wishCount;
    private long watchedCount;
    private long historyCount;
}
