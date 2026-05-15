package com.example.WonkaoTalk.domain.user.dto;

import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record UserResponse(
    Long userId,
    String nickname,
    String name,
    String phone,
    String image,
    Gender gender,
    LocalDate birthDate,
    boolean marketingAgree
) {

  public static UserResponse from(User user) {
    return UserResponse.builder()
        .userId(user.getId())
        .nickname(user.getNickname())
        .name(user.getName())
        .phone(user.getPhone())
        .image(user.getImage())
        .gender(user.getGender())
        .birthDate(user.getBirthDate())
        .marketingAgree(user.isMarketingAgree())
        .build();
  }

}
