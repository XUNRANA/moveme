package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieActor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MovieActorMapper extends BaseMapper<MovieActor> {
}
