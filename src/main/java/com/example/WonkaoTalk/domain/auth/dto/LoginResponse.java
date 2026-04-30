package com.example.WonkaoTalk.domain.auth.dto;

import lombok.Builder;

@Builder
public record LoginResponse(
    TokenInfo tokenInfo,
    UserInfo userInfo
) {

  @Builder
  public record TokenInfo(
      String accessToken,
      String grantType,
      Integer expiresIn
  ) {

    public static TokenInfo of(String accessToken, Integer expiresIn) {
      return TokenInfo.builder()
          .accessToken(accessToken)
          .grantType("Bearer")
          .expiresIn(expiresIn)
          .build();
    }
  }

  @Builder
  public record UserInfo(
      Long authId,
      Long userId,
      String nickname,
      String image
  ) {

  }
}
