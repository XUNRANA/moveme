package com.moveme.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.common.result.Result;
import com.moveme.module.user.dto.UserLoginDTO;
import com.moveme.module.user.dto.UserRegisterDTO;
import com.moveme.module.user.entity.Favorite;
import com.moveme.module.user.entity.Rating;
import com.moveme.module.user.service.UserService;
import com.moveme.module.user.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/auth/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/auth/login")
    public Result<TokenVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/auth/refresh")
    public Result<TokenVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return Result.success(userService.refreshToken(refreshToken));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/users/me")
    public Result<UserVO> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getCurrentUser(userId));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/users/me")
    public Result<Void> updateProfile(Authentication authentication,
                                      @RequestBody Map<String, String> body) {
        Long userId = (Long) authentication.getPrincipal();
        userService.updateProfile(userId, body.get("nickname"), body.get("email"));
        return Result.success();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/users/me/avatar")
    public Result<String> uploadAvatar(Authentication authentication,
                                       @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.uploadAvatar(userId, file));
    }

    @Operation(summary = "获取用户统计")
    @GetMapping("/users/me/stats")
    public Result<UserStatsVO> getUserStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getUserStats(userId));
    }

    @Operation(summary = "获取用户评分列表")
    @GetMapping("/users/me/ratings")
    public Result<IPage<UserRatingVO>> getUserRatings(Authentication authentication,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getUserRatings(userId, page, size));
    }

    @Operation(summary = "获取用户收藏列表")
    @GetMapping("/users/me/favorites")
    public Result<IPage<UserFavoriteVO>> getUserFavorites(Authentication authentication,
                                                           @RequestParam(required = false) Integer status,
                                                           @RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getUserFavorites(userId, status, page, size));
    }

    @Operation(summary = "获取用户浏览历史")
    @GetMapping("/users/me/history")
    public Result<IPage<UserHistoryVO>> getUserHistory(Authentication authentication,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getUserHistory(userId, page, size));
    }

    @Operation(summary = "获取用户口味分析")
    @GetMapping("/users/me/taste")
    public Result<UserTasteVO> getUserTaste(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.getUserTaste(userId));
    }

    // ─── 收藏 ───

    @Operation(summary = "查询收藏状态")
    @GetMapping("/users/me/favorites/check")
    public Result<Favorite> checkFavorite(Authentication authentication,
                                          @RequestParam Long movieId) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.checkFavorite(userId, movieId));
    }

    @Operation(summary = "添加收藏")
    @PostMapping("/users/me/favorites")
    public Result<Void> addFavorite(Authentication authentication,
                                    @RequestBody Map<String, Object> body) {
        Long userId = (Long) authentication.getPrincipal();
        Long movieId = ((Number) body.get("movieId")).longValue();
        Integer status = (Integer) body.get("status");
        userService.addFavorite(userId, movieId, status);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/users/me/favorites")
    public Result<Void> removeFavorite(Authentication authentication,
                                       @RequestParam Long movieId) {
        Long userId = (Long) authentication.getPrincipal();
        userService.removeFavorite(userId, movieId);
        return Result.success();
    }

    // ─── 评分 ───

    @Operation(summary = "查询评分状态")
    @GetMapping("/users/me/ratings/check")
    public Result<Rating> checkRating(Authentication authentication,
                                      @RequestParam Long movieId) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(userService.checkRating(userId, movieId));
    }

    @Operation(summary = "保存评分")
    @PostMapping("/users/me/ratings")
    public Result<Void> saveRating(Authentication authentication,
                                   @RequestBody Map<String, Object> body) {
        Long userId = (Long) authentication.getPrincipal();
        Long movieId = ((Number) body.get("movieId")).longValue();
        Integer score = (Integer) body.get("score");
        String comment = (String) body.get("comment");
        userService.saveRating(userId, movieId, score, comment);
        return Result.success();
    }

    @Operation(summary = "删除评分")
    @DeleteMapping("/users/me/ratings")
    public Result<Void> deleteRating(Authentication authentication,
                                     @RequestParam Long movieId) {
        Long userId = (Long) authentication.getPrincipal();
        userService.deleteRating(userId, movieId);
        return Result.success();
    }

    // ─── 浏览记录 ───

    @Operation(summary = "记录浏览")
    @PostMapping("/users/me/history")
    public Result<Void> recordView(Authentication authentication,
                                   @RequestBody Map<String, Object> body) {
        Long userId = (Long) authentication.getPrincipal();
        Long movieId = ((Number) body.get("movieId")).longValue();
        userService.recordView(userId, movieId);
        return Result.success();
    }
}
