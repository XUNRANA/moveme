package com.moveme.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.user.entity.UserPersonPref;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserPersonPrefMapper extends BaseMapper<UserPersonPref> {

    @Select("SELECT upp.person_id, p.name AS personName, upp.role_kind AS roleKind, upp.score " +
            "FROM user_person_pref upp " +
            "JOIN persons p ON p.id = upp.person_id " +
            "WHERE upp.user_id = #{userId} " +
            "ORDER BY upp.score DESC")
    List<Map<String, Object>> selectWithPersonName(@Param("userId") Long userId);
}
