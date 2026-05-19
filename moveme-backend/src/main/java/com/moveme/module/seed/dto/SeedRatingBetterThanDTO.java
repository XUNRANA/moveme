package com.moveme.module.seed.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeedRatingBetterThanDTO {
    private BigDecimal percentage;
    /** "犯罪" */
    private String genre;
    /** "犯罪片" */
    private String genreLabel;
    private String url;
}
