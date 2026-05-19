package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieTop250;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MovieTop250Mapper extends BaseMapper<MovieTop250> {

    @Select("SELECT * FROM movie_top250 WHERE movie_id = #{movieId}")
    MovieTop250 selectByMovieId(@Param("movieId") Long movieId);
}
