package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.enums.Role;
import lombok.Builder;

@Builder
public record SocialLoginDto(
    Long authId,
    Long userId,
    Long sellerId,
    Role role
) {

  public static SocialLoginDto of(Long authId, Long userId, Long sellerId, Role role) {
    return SocialLoginDto.builder()
        .authId(authId)
        .userId(userId)
        .sellerId(sellerId)
        .role(role)
        .build();
  }
}
