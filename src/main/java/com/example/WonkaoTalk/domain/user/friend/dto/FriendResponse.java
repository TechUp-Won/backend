package com.example.WonkaoTalk.domain.user.friend.dto;

import com.example.WonkaoTalk.domain.user.friend.entity.Friend;
import com.example.WonkaoTalk.domain.user.friend.enums.FriendStatus;
import lombok.Builder;

@Builder
public record FriendResponse(
    Long friendId,
    Long targetId,
    String name,
    String alias,
    String memo,
    FriendStatus status,
    boolean isFavorite
) {

  public static FriendResponse from(Friend friend) {
    return FriendResponse.builder()
        .friendId(friend.getId())
        .targetId(friend.getTarget().getId())
        .name(friend.getTarget().getName())
        .alias(friend.getAlias())
        .memo(friend.getMemo())
        .status(friend.getStatus())
        .isFavorite(friend.isFavorite())
        .build();
  }
}
