package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;

public record TokenDto(
    String accessToken,
    String refreshToken,
    long accessExpirationTime,
    Auth auth,
    String profileName
) {

}
