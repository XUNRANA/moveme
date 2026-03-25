package com.moveme.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.common.exception.BusinessException;
import com.moveme.common.result.ResultCode;
import com.moveme.common.util.JwtUtil;
import com.moveme.module.user.dto.UserLoginDTO;
import com.moveme.module.user.dto.UserRegisterDTO;
import com.moveme.module.user.entity.User;
import com.moveme.module.user.mapper.UserMapper;
import com.moveme.module.user.service.UserService;
import com.moveme.module.user.vo.TokenVO;
import com.moveme.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(UserRegisterDTO dto) {
        // 检查用户名唯一
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 检查邮箱唯一
        if (StringUtils.hasText(dto.getEmail()) &&
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, dto.getEmail())) > 0) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public TokenVO login(UserLoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.WRONG_CREDENTIALS);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String roleName = user.getRole() == 1 ? "ADMIN" : "USER";
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleName);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 存储 refresh token 到 Redis
        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                7, TimeUnit.DAYS);

        return new TokenVO(accessToken, refreshToken);
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token已过期");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String storedToken = (String) redisTemplate.opsForValue()
                .get(RedisKeyConstants.REFRESH_TOKEN_PREFIX + userId);

        if (!refreshToken.equals(storedToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token无效");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String roleName = user.getRole() == 1 ? "ADMIN" : "USER";
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleName);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_PREFIX + userId,
                newRefreshToken,
                7, TimeUnit.DAYS);

        return new TokenVO(newAccessToken, newRefreshToken);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public void updateProfile(Long userId, String nickname, String email) {
        User user = new User();
        user.setId(userId);
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        }
        if (StringUtils.hasText(email)) {
            // 检查邮箱唯一性
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email)
                    .ne(User::getId, userId));
            if (count > 0) {
                throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(email);
        }
        userMapper.updateById(user);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
