package com.moveme.module.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moveme.module.movie.entity.CommentVote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentVoteMapper extends BaseMapper<CommentVote> {
}
