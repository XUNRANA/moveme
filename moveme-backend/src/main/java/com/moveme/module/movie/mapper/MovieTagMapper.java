package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieTagMapper extends BaseMapper<MovieTag> {

    @Select("SELECT t.name FROM movie_tag mt JOIN tags t ON t.id = mt.tag_id WHERE mt.movie_id = #{movieId}")
    List<String> selectTagNamesByMovieId(@Param("movieId") Long movieId);
}
