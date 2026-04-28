package com.moveme.module.movie.vo;

import lombok.Data;

@Data
public class PersonBriefVO {

    private Long id;
    private String name;
    private String nameEn;
    private String avatarUrl;
    private String avatarLocalPath;
    private String profileUrl;
    private String roleName;
}
