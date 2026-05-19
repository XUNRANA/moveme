package com.moveme.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.user.entity.UserGenrePref;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserGenrePrefMapper extends BaseMapper<UserGenrePref> {

    @Select("SELECT ugp.genre_id, g.name AS genreName, ugp.score " +
            "FROM user_genre_pref ugp " +
            "JOIN genres g ON g.id = ugp.genre_id " +
            "WHERE ugp.user_id = #{userId} " +
            "ORDER BY ugp.score DESC")
    List<Map<String, Object>> selectWithGenreName(@Param("userId") Long userId);
}
