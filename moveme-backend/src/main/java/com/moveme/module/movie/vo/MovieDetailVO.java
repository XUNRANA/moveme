package com.moveme.module.movie.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MovieDetailVO extends MovieVO {

    private LocalDate releaseDate;
    private String imdbId;
    private List<String> countries;
    private List<String> languages;
    private List<PersonBriefVO> directors;
    private List<PersonBriefVO> actors;
    private List<PersonBriefVO> writers;
}
