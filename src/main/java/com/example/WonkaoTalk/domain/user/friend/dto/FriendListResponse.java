package com.example.WonkaoTalk.domain.user.friend.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record FriendListResponse(
    List<FriendInfoDTO> friends,
    int totalCount
) {

  public static FriendListResponse of(List<FriendInfoDTO> friends) {
    return FriendListResponse.builder()
        .friends(friends)
        .totalCount(friends.size())
        .build();
  }
}
