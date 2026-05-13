package com.example.WonkaoTalk.domain.user.friend.dto;

import com.example.WonkaoTalk.domain.user.friend.entity.Friend;
import java.util.List;
import lombok.Builder;

public class FriendResponse {

  @Builder
  public record AddResponse(
      Long friendId
  ) {

    public static AddResponse of(Long friendId) {
      return AddResponse.builder()
          .friendId(friendId)
          .build();
    }
  }

  @Builder
  public record Info(
      Long friendId,
      Long targetId,
      String name,
      String alias,
      String image,
      boolean isFavorite
  ) {

    public static Info from(Friend friend) {
      return Info.builder()
          .friendId(friend.getId())
          .targetId(friend.getTarget().getId())
          .name(friend.getTarget().getName())
          .alias(friend.getAlias())
          .image(friend.getTarget().getImage())
          .isFavorite(friend.isFavorite())
          .build();
    }
  }

  public record FriendListResponse(
      List<Info> friends,
      int totalCount
  ) {

    public static FriendListResponse of(List<Info> friends) {
      return new FriendListResponse(friends, friends.size());
    }
  }

  @Builder
  public record UpdateResponse(
      Long friendId,
      Long targetId,
      String alias,
      String memo,
      boolean isFavorite
  ) {

    public static UpdateResponse from(Friend friend) {
      return UpdateResponse.builder()
          .friendId(friend.getId())
          .targetId(friend.getTarget().getId())
          .alias(friend.getAlias())
          .memo(friend.getMemo())
          .isFavorite(friend.isFavorite())
          .build();
    }
  }
}
