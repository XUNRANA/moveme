package com.moveme.module.seed.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeedRatingBreakdownDTO {
    /** 1..5 */
    private Integer star;
    /** "5星" */
    private String label;
    /** "力荐" */
    private String text;
    private BigDecimal percentage;
}
