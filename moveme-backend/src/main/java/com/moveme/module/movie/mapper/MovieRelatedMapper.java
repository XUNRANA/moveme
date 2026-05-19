package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieRelated;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MovieRelatedMapper extends BaseMapper<MovieRelated> {

    @Update("UPDATE movie_related r " +
            "JOIN movies m ON r.related_douban_id = m.douban_id " +
            "SET r.related_movie_id = m.id " +
            "WHERE r.related_movie_id IS NULL")
    int backfillRelatedMovieIds();

    @Select("SELECT * FROM movie_related WHERE movie_id = #{movieId} ORDER BY sort_order")
    List<MovieRelated> selectByMovieId(@Param("movieId") Long movieId);
}
