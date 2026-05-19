package com.moveme.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moveme.common.exception.BusinessException;
import com.moveme.common.result.ResultCode;
import com.moveme.module.admin.service.AdminService;
import com.moveme.module.crawler.entity.CrawlLog;
import com.moveme.module.crawler.mapper.CrawlLogMapper;
import com.moveme.module.movie.mapper.GenreMapper;
import com.moveme.module.movie.mapper.MovieCommentMapper;
import com.moveme.module.movie.mapper.MovieMapper;
import com.moveme.module.movie.mapper.PersonMapper;
import com.moveme.module.seed.entity.ImportLog;
import com.moveme.module.seed.mapper.ImportLogMapper;
import com.moveme.module.user.entity.RecommendationLog;
import com.moveme.module.user.entity.User;
import com.moveme.module.user.mapper.FavoriteMapper;
import com.moveme.module.user.mapper.RatingMapper;
import com.moveme.module.user.mapper.RecommendationLogMapper;
import com.moveme.module.user.mapper.SearchHistoryMapper;
import com.moveme.module.user.mapper.UserMapper;
import com.moveme.module.user.mapper.ViewHistoryMapper;
import com.moveme.module.admin.vo.AdminStatsVO;
import com.moveme.module.admin.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final MovieMapper movieMapper;
    private final UserMapper userMapper;
    private final RatingMapper ratingMapper;
    private final CrawlLogMapper crawlLogMapper;
    private final ImportLogMapper importLogMapper;
    private final RecommendationLogMapper recommendationLogMapper;
    private final FavoriteMapper favoriteMapper;
    private final ViewHistoryMapper viewHistoryMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final PersonMapper personMapper;
    private final GenreMapper genreMapper;
    private final MovieCommentMapper movieCommentMapper;

    @Override
    public AdminStatsVO getStats() {
        AdminStatsVO vo = new AdminStatsVO();
        vo.setMovieCount(movieMapper.selectCount(null));
        vo.setUserCount(userMapper.selectCount(null));
        vo.setTodayNewUsers(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreatedAt, LocalDate.now().atStartOfDay())));
        vo.setRatingCount(ratingMapper.selectCount(null));

        // 最近一次爬取日志
        CrawlLog lastCrawl = crawlLogMapper.selectOne(
                new LambdaQueryWrapper<CrawlLog>()
                        .orderByDesc(CrawlLog::getStartedAt)
                        .last("LIMIT 1"));
        if (lastCrawl != null) {
            vo.setLastCrawlStatus(lastCrawl.getStatus() == 1 ? "成功" : "失败");
            vo.setLastCrawlTime(lastCrawl.getStartedAt());
        }

        vo.setRecoLogCount(recommendationLogMapper.selectCount(null));
        vo.setFavoriteCount(favoriteMapper.selectCount(null));
        vo.setViewHistoryCount(viewHistoryMapper.selectCount(null));
        vo.setSearchHistoryCount(searchHistoryMapper.selectCount(null));
        vo.setPersonCount(personMapper.selectCount(null));
        vo.setGenreCount(genreMapper.selectCount(null));
        vo.setCrawlLogCount(crawlLogMapper.selectCount(null));
        vo.setImportLogCount(importLogMapper.selectCount(null));
        vo.setMovieCommentCount(movieCommentMapper.selectCount(null));
        return vo;
    }

    @Override
    public IPage<AdminUserVO> listUsers(String keyword, Integer role, Integer status, long page, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        IPage<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        return userPage.convert(this::toAdminUserVO);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    public void updateUserRole(Long userId, Integer role) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        User update = new User();
        update.setId(userId);
        update.setRole(role);
        userMapper.updateById(update);
    }

    @Override
    public IPage<CrawlLog> listCrawlLogs(long page, long size) {
        return crawlLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CrawlLog>().orderByDesc(CrawlLog::getStartedAt));
    }

    @Override
    public IPage<ImportLog> listImportLogs(long page, long size) {
        return importLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ImportLog>().orderByDesc(ImportLog::getStartedAt));
    }

    @Override
    public IPage<RecommendationLog> listRecoLogs(long page, long size) {
        return recommendationLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<RecommendationLog>().orderByDesc(RecommendationLog::getCreatedAt));
    }

    private AdminUserVO toAdminUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
