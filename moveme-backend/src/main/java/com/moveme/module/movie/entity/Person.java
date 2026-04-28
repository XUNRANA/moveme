package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("persons")
public class Person {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String doubanPersonId;
    private String name;
    private String nameEn;
    private String avatarUrl;
    private String avatarLocalPath;
    private String profileUrl;
    private Integer gender;
    private LocalDate birthDate;
    private String birthPlace;
    private String bio;

    private Integer movieCount;
    private BigDecimal avgMovieRating;
}
