package com.moveme.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.user.entity.Favorite;
import com.moveme.module.user.vo.UserFavoriteVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT f.id, f.movie_id AS movieId, m.title, m.poster_url AS posterUrl, " +
            "m.poster_local_path AS posterLocalPath, m.douban_rating AS doubanRating, " +
            "m.year, f.status, f.created_at AS createdAt " +
            "FROM favorites f " +
            "JOIN movies m ON m.id = f.movie_id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<UserFavoriteVO> selectByUserIdWithMovie(@Param("userId") Long userId);

    @Select("SELECT f.id, f.movie_id AS movieId, m.title, m.poster_url AS posterUrl, " +
            "m.poster_local_path AS posterLocalPath, m.douban_rating AS doubanRating, " +
            "m.year, f.status, f.created_at AS createdAt " +
            "FROM favorites f " +
            "JOIN movies m ON m.id = f.movie_id " +
            "WHERE f.user_id = #{userId} AND f.status = #{status} " +
            "ORDER BY f.created_at DESC")
    List<UserFavoriteVO> selectByUserIdAndStatusWithMovie(@Param("userId") Long userId,
                                                          @Param("status") Integer status);
}
