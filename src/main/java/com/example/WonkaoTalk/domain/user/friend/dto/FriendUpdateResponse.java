package com.example.WonkaoTalk.domain.user.friend.dto;

import com.example.WonkaoTalk.domain.user.friend.entity.Friend;
import lombok.Builder;

@Builder
public record FriendUpdateResponse(
    Long friendId,
    Long targetId,
    String alias,
    String memo,
    boolean isFavorite
) {

  public static FriendUpdateResponse from(Friend friend) {
    return FriendUpdateResponse.builder()
        .friendId(friend.getId())
        .targetId(friend.getTarget().getId())
        .alias(friend.getAlias())
        .memo(friend.getMemo())
        .isFavorite(friend.isFavorite())
        .build();
  }
}
