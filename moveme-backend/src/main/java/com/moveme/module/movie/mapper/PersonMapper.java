package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.Person;
import com.moveme.module.movie.vo.PersonBriefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
