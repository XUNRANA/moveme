package com.moveme.module.user.service.impl;

import com.moveme.common.constant.RedisKeyConstants;
import com.moveme.common.exception.BusinessException;
import com.moveme.common.result.ResultCode;
import com.moveme.common.util.JwtUtil;
import com.moveme.module.user.dto.UserLoginDTO;
import com.moveme.module.user.dto.UserRegisterDTO;
import com.moveme.module.user.entity.User;
import com.moveme.module.user.mapper.UserMapper;
import com.moveme.module.user.vo.TokenVO;
import com.moveme.module.user.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldInsertUserWithEncodedPasswordAndDefaultNickname() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("plain-pass");
        dto.setEmail("alice@example.com");

        when(userMapper.selectCount(any())).thenReturn(0L, 0L);
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");

        userService.register(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals("alice", savedUser.getUsername()),
                () -> assertEquals("encoded-pass", savedUser.getPassword()),
                () -> assertEquals("alice@example.com", savedUser.getEmail()),
                () -> assertEquals("alice", savedUser.getNickname()),
                () -> assertEquals(0, savedUser.getRole()),
                () -> assertEquals(1, savedUser.getStatus())
        );
    }

    @Test
    void register_shouldThrowBusinessException_whenUsernameExists() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("admin");
        dto.setPassword("secret123");

        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(dto));

        assertAll(
                () -> assertEquals(ResultCode.USER_ALREADY_EXISTS.getCode(), ex.getCode()),
                () -> assertEquals(ResultCode.USER_ALREADY_EXISTS.getMessage(), ex.getMessage())
        );
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void login_shouldReturnTokensAndPersistRefreshToken_whenCredentialsValid() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("encoded-pass");
        user.setRole(1);
        user.setStatus(1);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("admin123", "encoded-pass")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "admin", "ADMIN")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenVO tokenVO = userService.login(dto);

        assertAll(
                () -> assertEquals("access-token", tokenVO.getAccessToken()),
                () -> assertEquals("refresh-token", tokenVO.getRefreshToken())
        );
        verify(valueOperations).set(
                eq(RedisKeyConstants.REFRESH_TOKEN_PREFIX + 1L),
                eq("refresh-token"),
                eq(7L),
                eq(TimeUnit.DAYS)
        );
    }

    @Test
    void refreshToken_shouldThrowBusinessException_whenStoredTokenMismatch() {
        when(jwtUtil.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtUtil.getUserId("refresh-token")).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyConstants.REFRESH_TOKEN_PREFIX + 1L)).thenReturn("another-token");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refreshToken("refresh-token"));

        assertAll(
                () -> assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode()),
                () -> assertEquals("Refresh Token无效", ex.getMessage())
        );
    }

    @Test
    void getCurrentUser_shouldMapEntityToViewObject() {
        User user = new User();
        user.setId(7L);
        user.setUsername("tester");
        user.setEmail("tester@example.com");
        user.setNickname("测试用户");
        user.setAvatarUrl("https://img.example.com/avatar.png");
        user.setRole(0);
        user.setCreatedAt(LocalDateTime.of(2026, 3, 31, 10, 0));

        when(userMapper.selectById(7L)).thenReturn(user);

        UserVO userVO = userService.getCurrentUser(7L);

        assertAll(
                () -> assertEquals(7L, userVO.getId()),
                () -> assertEquals("tester", userVO.getUsername()),
                () -> assertEquals("tester@example.com", userVO.getEmail()),
                () -> assertEquals("测试用户", userVO.getNickname()),
                () -> assertEquals("https://img.example.com/avatar.png", userVO.getAvatarUrl()),
                () -> assertEquals(0, userVO.getRole()),
                () -> assertTrue(userVO.getCreatedAt().isEqual(LocalDateTime.of(2026, 3, 31, 10, 0)))
        );
    }
}
