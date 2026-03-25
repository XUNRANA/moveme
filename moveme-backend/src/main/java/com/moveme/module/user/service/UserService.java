package com.moveme.module.user.service;

import com.moveme.module.user.dto.UserLoginDTO;
import com.moveme.module.user.dto.UserRegisterDTO;
import com.moveme.module.user.vo.TokenVO;
import com.moveme.module.user.vo.UserVO;

public interface UserService {

    void register(UserRegisterDTO dto);

    TokenVO login(UserLoginDTO dto);

    TokenVO refreshToken(String refreshToken);

    UserVO getCurrentUser(Long userId);

    void updateProfile(Long userId, String nickname, String email);
}
