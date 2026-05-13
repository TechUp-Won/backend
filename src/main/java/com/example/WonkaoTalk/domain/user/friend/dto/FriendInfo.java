package com.example.WonkaoTalk.domain.user.friend.dto;

import com.example.WonkaoTalk.domain.user.friend.entity.Friend;
import lombok.Builder;

@Builder
public record FriendInfo(
    Long friendId,
    Long targetId,
    String name,
    String alias,
    String image,
    boolean isFavorite
) {

  public static FriendInfo from(Friend friend) {
    return FriendInfo.builder()
        .friendId(friend.getId())
        .targetId(friend.getTarget().getId())
        .name(friend.getTarget().getName())
        .alias(friend.getAlias())
        .image(friend.getTarget().getImage())
        .isFavorite(friend.isFavorite())
        .build();
  }
}
