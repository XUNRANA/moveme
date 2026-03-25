package com.moveme.module.user.controller;

import com.moveme.common.result.Result;
import com.moveme.module.user.dto.UserLoginDTO;
import com.moveme.module.user.dto.UserRegisterDTO;
import com.moveme.module.user.service.UserService;
import com.moveme.module.user.vo.TokenVO;
import com.moveme.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
