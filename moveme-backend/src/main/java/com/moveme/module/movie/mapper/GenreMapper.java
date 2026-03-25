package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.Genre;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GenreMapper extends BaseMapper<Genre> {
}
