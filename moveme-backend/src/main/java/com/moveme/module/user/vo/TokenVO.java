package com.moveme.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVO {
    private String accessToken;
    private String refreshToken;
}
