package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieRatingDist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieRatingDistMapper extends BaseMapper<MovieRatingDist> {

    @Select("SELECT * FROM movie_rating_dist WHERE movie_id = #{movieId} ORDER BY star DESC")
    List<MovieRatingDist> selectByMovieId(@Param("movieId") Long movieId);
}
