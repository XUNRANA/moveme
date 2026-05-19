package com.moveme.module.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.module.user.dto.UserLoginDTO;
import com.moveme.module.user.dto.UserRegisterDTO;
import com.moveme.module.user.entity.Favorite;
import com.moveme.module.user.entity.Rating;
import com.moveme.module.user.vo.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    void register(UserRegisterDTO dto);

    TokenVO login(UserLoginDTO dto);

    TokenVO refreshToken(String refreshToken);

    UserVO getCurrentUser(Long userId);

    void updateProfile(Long userId, String nickname, String email);

    String uploadAvatar(Long userId, MultipartFile file);

    UserStatsVO getUserStats(Long userId);

    IPage<UserRatingVO> getUserRatings(Long userId, long page, long size);

    IPage<UserFavoriteVO> getUserFavorites(Long userId, Integer status, long page, long size);

    IPage<UserHistoryVO> getUserHistory(Long userId, long page, long size);

    UserTasteVO getUserTaste(Long userId);

    // ─── 收藏 ───

    Favorite checkFavorite(Long userId, Long movieId);

    void addFavorite(Long userId, Long movieId, Integer status);

    void removeFavorite(Long userId, Long movieId);

    // ─── 评分 ───

    Rating checkRating(Long userId, Long movieId);

    void saveRating(Long userId, Long movieId, Integer score, String comment);

    void deleteRating(Long userId, Long movieId);

    // ─── 浏览记录 ───

    void recordView(Long userId, Long movieId);
}
