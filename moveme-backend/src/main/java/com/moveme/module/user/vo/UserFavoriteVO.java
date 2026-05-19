package com.moveme.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserFavoriteVO {
    private Long id;
    private Long movieId;
    private String title;
    private String posterUrl;
    private String posterLocalPath;
    private BigDecimal doubanRating;
    private Short year;
    private List<String> genres;
    private Integer status;
    private LocalDateTime createdAt;
}
