package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.user.entity.User;

public record TokenDto(
    String accessToken,
    String refreshToken,
    long accessExpirationTime,
    Auth auth,
    User user
) {

}
