package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MoviePlayLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MoviePlayLinkMapper extends BaseMapper<MoviePlayLink> {

    @Select("SELECT platform, url FROM movie_play_link WHERE movie_id = #{movieId}")
    List<MoviePlayLink> selectByMovieId(@Param("movieId") Long movieId);
}
