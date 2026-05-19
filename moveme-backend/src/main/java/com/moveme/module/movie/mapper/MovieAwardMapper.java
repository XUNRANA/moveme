package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieAward;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieAwardMapper extends BaseMapper<MovieAward> {

    @Select("SELECT * FROM movie_award WHERE movie_id = #{movieId}")
    List<MovieAward> selectByMovieId(@Param("movieId") Long movieId);
}
