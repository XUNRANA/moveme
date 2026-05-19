package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieAka;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieAkaMapper extends BaseMapper<MovieAka> {

    @Select("SELECT title FROM movie_aka WHERE movie_id = #{movieId}")
    List<String> selectTitlesByMovieId(@Param("movieId") Long movieId);
}
