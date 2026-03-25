package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.MovieGenre;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MovieGenreMapper extends BaseMapper<MovieGenre> {

    @Insert("INSERT IGNORE INTO movie_genre (movie_id, genre_id) VALUES (#{movieId}, #{genreId})")
    int insertIgnore(@Param("movieId") Long movieId, @Param("genreId") Integer genreId);

    @Delete("DELETE FROM movie_genre WHERE movie_id = #{movieId}")
    int deleteByMovieId(@Param("movieId") Long movieId);
}
