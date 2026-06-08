package com.example.WonkaoTalk.domain.friend.dto;

import com.example.WonkaoTalk.domain.friend.entity.Friend;
import lombok.Builder;

@Builder
public record FriendInfoDTO(
    Long friendId,
    Long targetId,
    String name,
    String alias,
    String image,
    boolean isFavorite
) {

  public static FriendInfoDTO from(Friend friend) {
    return FriendInfoDTO.builder()
        .friendId(friend.getId())
        .targetId(friend.getTarget().getId())
        .name(friend.getTarget().getName())
        .alias(friend.getAlias())
        .image(friend.getTarget().getImage())
        .isFavorite(friend.isFavorite())
        .build();
  }
}
