package com.example.WonkaoTalk.domain.user.friend.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record FriendListResponse(
    List<FriendInfo> friends,
    int totalCount
) {

  public static FriendListResponse of(List<FriendInfo> friends) {
    return FriendListResponse.builder()
        .friends(friends)
        .totalCount(friends.size())
        .build();
  }
}
