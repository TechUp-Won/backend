package com.example.WonkaoTalk.domain.auth.dto;

import lombok.Builder;

@Builder
public record AuthUserInfoDto(
    String profileName,
    Long userId,
    Long sellerId
) {

  public static AuthUserInfoDto of(String profileName, Long userId, Long sellerId) {
    return AuthUserInfoDto.builder()
        .profileName(profileName)
        .userId(userId)
        .sellerId(sellerId)
        .build();
  }

}
