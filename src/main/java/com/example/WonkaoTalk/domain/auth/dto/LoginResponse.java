package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.user.entity.User;
import lombok.Builder;

@Builder
public record LoginResponse(
    TokenInfo tokenInfo,
    UserInfo userInfo
) {

  public static LoginResponse of(
      String accessToken, Long expiresIn, Auth auth, User user
  ) {
    return LoginResponse.builder()
        .tokenInfo(TokenInfo.of(accessToken, expiresIn))
        .userInfo(UserInfo.of(auth, user))
        .build();
  }


  @Builder
  public record TokenInfo(
      String accessToken,
      String grantType,
      Long expiresIn
  ) {

    public static TokenInfo of(String accessToken, Long expiresIn) {
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

    public static UserInfo of(Auth auth, User user) {
      return UserInfo.builder()
          .authId(user.getId())
          .userId(user.getId())
          .nickname(user.getNickname())
          .image(user.getImage())
          .build();
    }
  }
}
