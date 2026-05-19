package com.moveme.module.movie.vo;

import lombok.Data;

import java.util.List;

@Data
public class DiscoverSectionVO {

    private String key;
    private String title;
    private List<MovieVO> movies;
}
