package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.Person;
import com.moveme.module.movie.vo.PersonBriefVO;
import com.moveme.module.movie.vo.PersonDetailVO.FilmographyItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PersonMapper extends BaseMapper<Person> {

    @Select("""
            SELECT p.id, p.name, p.name_en AS nameEn, p.avatar_url AS avatarUrl,
                   p.avatar_local_path AS avatarLocalPath, p.profile_url AS profileUrl,
                   ma.role_name AS roleName
            FROM movie_actor ma
            JOIN persons p ON p.id = ma.person_id
            WHERE ma.movie_id = #{movieId}
            ORDER BY ma.sort_order ASC
            """)
    List<PersonBriefVO> selectActorsByMovieId(@Param("movieId") Long movieId);

    @Select("""
            SELECT p.id, p.name, p.name_en AS nameEn, p.avatar_url AS avatarUrl,
                   p.avatar_local_path AS avatarLocalPath, p.profile_url AS profileUrl
            FROM movie_director md
            JOIN persons p ON p.id = md.person_id
            WHERE md.movie_id = #{movieId}
            ORDER BY md.sort_order ASC
            """)
    List<PersonBriefVO> selectDirectorsByMovieId(@Param("movieId") Long movieId);

    @Select("""
            SELECT p.id, p.name, p.name_en AS nameEn, p.avatar_url AS avatarUrl,
                   p.avatar_local_path AS avatarLocalPath, p.profile_url AS profileUrl
            FROM movie_writer mw
            JOIN persons p ON p.id = mw.person_id
            WHERE mw.movie_id = #{movieId}
            ORDER BY mw.sort_order ASC
            """)
    List<PersonBriefVO> selectWritersByMovieId(@Param("movieId") Long movieId);

    /**
     * 全量重算 persons.movie_count =
     *   该 person 在 movie_director / movie_writer / movie_actor 三表里 distinct 出现的电影数。
     * Phase B 全量导入完毕后调一次即可（毫秒级）。
     */
    @Update("""
            UPDATE persons p
            LEFT JOIN (
                SELECT person_id, COUNT(DISTINCT movie_id) cnt FROM (
                    SELECT person_id, movie_id FROM movie_director
                    UNION ALL
                    SELECT person_id, movie_id FROM movie_writer
                    UNION ALL
                    SELECT person_id, movie_id FROM movie_actor
                ) u GROUP BY person_id
            ) s ON s.person_id = p.id
            SET p.movie_count = COALESCE(s.cnt, 0)
            """)
    int recalcMovieCount();

    /**
     * 全量重算 persons.avg_movie_rating = 这些电影 douban_rating 的均值。
     */
    @Update("""
            UPDATE persons p
            LEFT JOIN (
                SELECT person_id, AVG(douban_rating) avg_r FROM (
                    SELECT t.person_id, m.douban_rating
                    FROM movie_actor t JOIN movies m ON m.id = t.movie_id
                    UNION ALL
                    SELECT t.person_id, m.douban_rating
                    FROM movie_director t JOIN movies m ON m.id = t.movie_id
                    UNION ALL
                    SELECT t.person_id, m.douban_rating
                    FROM movie_writer t JOIN movies m ON m.id = t.movie_id
                ) u WHERE douban_rating IS NOT NULL
                GROUP BY person_id
            ) s ON s.person_id = p.id
            SET p.avg_movie_rating = COALESCE(s.avg_r, 0)
            """)
    int recalcAvgMovieRating();

    @Select("""
            SELECT m.id AS movieId, m.title, m.poster_local_path AS posterUrl,
                   m.douban_rating AS rating, m.year
            FROM movie_director md
            JOIN movies m ON m.id = md.movie_id
            WHERE md.person_id = #{personId} AND m.status = 1
            ORDER BY m.douban_rating DESC
            """)
    List<FilmographyItem> selectDirectedMoviesByPersonId(@Param("personId") Long personId);

    @Select("""
            SELECT m.id AS movieId, m.title, m.poster_local_path AS posterUrl,
                   m.douban_rating AS rating, m.year
            FROM movie_writer mw
            JOIN movies m ON m.id = mw.movie_id
            WHERE mw.person_id = #{personId} AND m.status = 1
            ORDER BY m.douban_rating DESC
            """)
    List<FilmographyItem> selectWrittenMoviesByPersonId(@Param("personId") Long personId);

    @Select("""
            SELECT m.id AS movieId, m.title, m.poster_local_path AS posterUrl,
                   m.douban_rating AS rating, m.year, ma.role_name AS roleName
            FROM movie_actor ma
            JOIN movies m ON m.id = ma.movie_id
            WHERE ma.person_id = #{personId} AND m.status = 1
            ORDER BY m.douban_rating DESC
            """)
    List<FilmographyItem> selectActedMoviesByPersonId(@Param("personId") Long personId);
}
