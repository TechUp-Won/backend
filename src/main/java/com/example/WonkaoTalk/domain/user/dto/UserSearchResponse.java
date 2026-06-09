package com.example.WonkaoTalk.domain.user.dto;

import lombok.Builder;

@Builder
public record UserSearchResponse(
    Long userId,
    String phone,
    String nickname,
    String image
) {

  public static UserSearchResponse of(Long userId, String phone, String nickname, String image) {
    return UserSearchResponse.builder()
        .userId(userId)
        .phone(phone)
        .nickname(nickname)
        .image(image)
        .build();
  }
}
