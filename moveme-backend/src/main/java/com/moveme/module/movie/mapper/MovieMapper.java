package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.Movie;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MovieMapper extends BaseMapper<Movie> {

    /** 仅更新本地海报路径（Phase B PosterFileImporter 复用） */
    @Update("UPDATE movies SET poster_local_path = #{localPath} WHERE id = #{id}")
    int updatePosterLocalPath(@Param("id") Long id, @Param("localPath") String localPath);

    @Select("SELECT mg.movie_id " +
            "FROM movie_genre mg " +
            "JOIN genres g ON g.id = mg.genre_id " +
            "WHERE g.name = #{genre}")
    List<Long> selectMovieIdsByGenre(@Param("genre") String genre);

    @Select("SELECT g.name " +
            "FROM movie_genre mg " +
            "JOIN genres g ON g.id = mg.genre_id " +
            "WHERE mg.movie_id = #{movieId} " +
            "ORDER BY g.name ASC")
    List<String> selectGenreNamesByMovieId(@Param("movieId") Long movieId);

    @Select("SELECT c.name FROM movie_country mc JOIN countries c ON c.id = mc.country_id " +
            "WHERE mc.movie_id = #{movieId} ORDER BY c.name ASC")
    List<String> selectCountryNamesByMovieId(@Param("movieId") Long movieId);

    @Select("SELECT l.name FROM movie_language ml JOIN languages l ON l.id = ml.language_id " +
            "WHERE ml.movie_id = #{movieId} ORDER BY l.name ASC")
    List<String> selectLanguageNamesByMovieId(@Param("movieId") Long movieId);

    @Select("SELECT COUNT(*) FROM movies " +
            "WHERE status = 1 " +
            "AND MATCH(title, title_cn, title_en, summary) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)")
    long countByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM movies " +
            "WHERE status = 1 " +
            "AND MATCH(title, title_cn, title_en, summary) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY douban_rating DESC, douban_votes DESC, updated_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Movie> fullTextSearch(@Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("limit") int limit);
}
