package com.moveme.module.movie.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MovieQueryDTO {

    @Size(max = 50, message = "genre is too long")
    private String genre;

    @Min(value = 1900, message = "year must be >= 1900")
    @Max(value = 2100, message = "year must be <= 2100")
    private Integer year;

    @DecimalMin(value = "0.0", message = "ratingMin must be >= 0")
    @DecimalMax(value = "10.0", message = "ratingMin must be <= 10")
    private BigDecimal ratingMin;

    @Size(max = 100, message = "keyword is too long")
    private String keyword;

    @Min(value = 1, message = "page must be >= 1")
    private Long page = 1L;

    @Min(value = 1, message = "size must be >= 1")
    @Max(value = 50, message = "size must be <= 50")
    private Long size = 12L;
}
