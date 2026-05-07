package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import lombok.Builder;

@Builder
public record LoginResponse(
    TokenInfo tokenInfo,
    UserInfo userInfo
) {

  public static LoginResponse of(
      String accessToken, Long expiresIn, Auth auth, String profileName
  ) {
    return LoginResponse.builder()
        .tokenInfo(TokenInfo.of(accessToken, expiresIn))
        .userInfo(UserInfo.of(auth, profileName))
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
      Role role,
      String profileName
  ) {

    public static UserInfo of(Auth auth, String profileName) {
      return UserInfo.builder()
          .authId(auth.getId())
          .role(auth.getRole())
          .profileName(profileName)
          .build();
    }
  }
}
