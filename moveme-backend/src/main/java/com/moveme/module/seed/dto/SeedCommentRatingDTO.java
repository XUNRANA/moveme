package com.moveme.module.seed.dto;

import lombok.Data;

@Data
public class SeedCommentRatingDTO {
    /** 1-5 (整数) 但豆瓣偶尔返回浮点 5.0 → 用 Double 接住，业务侧再转 Integer */
    private Double value;
    /** "力荐" / "推荐" / "还行" / ... */
    private String label;
}
