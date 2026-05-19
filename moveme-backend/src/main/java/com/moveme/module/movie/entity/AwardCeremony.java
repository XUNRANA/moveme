package com.moveme.module.movie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("award_ceremonies")
public class AwardCeremony {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;
    private String organization;
    private String description;
}
