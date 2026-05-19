package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieReleaseDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieReleaseDateMapper extends BaseMapper<MovieReleaseDate> {

    @Select("SELECT * FROM movie_release_date WHERE movie_id = #{movieId} ORDER BY release_at")
    List<MovieReleaseDate> selectByMovieId(@Param("movieId") Long movieId);
}
