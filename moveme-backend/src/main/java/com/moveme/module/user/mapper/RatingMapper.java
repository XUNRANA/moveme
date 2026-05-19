package com.moveme.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.user.entity.Rating;
import com.moveme.module.user.vo.UserRatingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    @Select("SELECT r.id, r.movie_id AS movieId, m.title, m.poster_url AS posterUrl, " +
            "m.poster_local_path AS posterLocalPath, r.score, r.comment, r.created_at AS createdAt " +
            "FROM ratings r " +
            "JOIN movies m ON m.id = r.movie_id " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.created_at DESC")
    List<UserRatingVO> selectByUserIdWithMovie(@Param("userId") Long userId);
}
