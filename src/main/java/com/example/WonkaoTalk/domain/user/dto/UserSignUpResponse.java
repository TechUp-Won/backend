package com.example.WonkaoTalk.domain.user.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.user.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record UserSignUpResponse(
    Long authId,
    Long userId,
    LocalDateTime createdAt
) {

  public static UserSignUpResponse of(Auth auth, User user) {
    return UserSignUpResponse.builder()
        .authId(auth.getId())
        .userId(user.getId())
        .createdAt(auth.getCreatedAt())
        .build();
  }
}
