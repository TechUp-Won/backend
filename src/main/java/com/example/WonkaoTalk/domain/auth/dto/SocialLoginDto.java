package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import lombok.Builder;

@Builder
public record SocialLoginDto(
    Auth auth,
    Long userId,
    Long sellerId
) {

  public SocialLoginDto of(Auth auth, Long userId, Long sellerId) {
    return SocialLoginDto.builder()
        .auth(auth)
        .userId(userId)
        .sellerId(sellerId)
        .build();
  }
}
