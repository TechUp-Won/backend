package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import lombok.Builder;

@Builder
public record TokenDto(
    String accessToken,
    String refreshToken,
    Long accessExpirationTime,
    Auth auth,
    String profileName
) {

  public static TokenDto of(String accessToken, String refreshToken, Long accessExpirationTime,
      Auth auth, String profileName) {
    return TokenDto.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .accessExpirationTime(accessExpirationTime)
        .auth(auth)
        .profileName(profileName)
        .build();
  }
}
