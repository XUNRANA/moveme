package com.moveme.module.seed.dto;

import lombok.Data;

@Data
public class SeedRelatedMovieDTO {
    private String subjectId;
    private String title;
    private Double rating;
    private String url;
    private String coverImage;
}
