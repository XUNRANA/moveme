package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("countries")
public class Country {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;
    private String code;
}
