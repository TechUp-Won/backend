package com.example.WonkaoTalk.domain.user.friend.dto;

import lombok.Builder;

@Builder
public record FriendAddResponse(
    Long friendId
) {

  public static FriendAddResponse of(Long friendId) {
    return FriendAddResponse.builder()
        .friendId(friendId)
        .build();
  }
}
