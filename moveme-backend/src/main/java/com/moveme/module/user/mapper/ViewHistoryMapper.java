package com.moveme.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.user.entity.ViewHistory;
import com.moveme.module.user.vo.UserHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ViewHistoryMapper extends BaseMapper<ViewHistory> {

    @Select("SELECT vh.id, vh.movie_id AS movieId, m.title, m.poster_url AS posterUrl, " +
            "m.poster_local_path AS posterLocalPath, vh.viewed_at AS viewedAt " +
            "FROM view_history vh " +
            "JOIN movies m ON m.id = vh.movie_id " +
            "WHERE vh.user_id = #{userId} " +
            "ORDER BY vh.viewed_at DESC")
    List<UserHistoryVO> selectByUserIdWithMovie(@Param("userId") Long userId);
}
