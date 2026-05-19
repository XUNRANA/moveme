package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieGenreRank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieGenreRankMapper extends BaseMapper<MovieGenreRank> {

    @Select("SELECT * FROM movie_genre_rank WHERE movie_id = #{movieId} ORDER BY percentile DESC")
    List<MovieGenreRank> selectByMovieId(@Param("movieId") Long movieId);
}
