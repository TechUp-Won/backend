package com.example.WonkaoTalk.domain.user.friend.dto;

import com.example.WonkaoTalk.domain.user.friend.enums.FriendStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FriendRequest {

  public record AddRequest(
      @NotNull(message = "추가할 대상의 ID는 필수입니다.")
      Long targetId
  ) {

  }

  public record UpdateRequest(
      @Size(max = 20, message = "별칭은 20자를 초과할 수 없습니다.")
      String alias,

      @Size(max = 200, message = "메모는 200자를 초과할 수 없습니다.")
      String memo
  ) {

  }

  public record StatusRequest(
      @NotNull(message = "변경할 상태값은 필수입니다.")
      FriendStatus status
  ) {

  }
}
