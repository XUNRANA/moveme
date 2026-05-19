package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moveme.module.movie.entity.MovieComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MovieCommentMapper extends BaseMapper<MovieComment> {

    @Select("SELECT * FROM movie_comment WHERE movie_id = #{movieId} ORDER BY votes DESC, posted_at DESC")
    List<MovieComment> selectByMovieIdHot(Page<MovieComment> page, @Param("movieId") Long movieId);

    @Select("SELECT * FROM movie_comment WHERE movie_id = #{movieId} ORDER BY posted_at DESC, votes DESC")
    List<MovieComment> selectByMovieIdNew(Page<MovieComment> page, @Param("movieId") Long movieId);
}
