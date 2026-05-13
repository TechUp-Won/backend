package com.example.WonkaoTalk.domain.user.friend.dto;

import com.example.WonkaoTalk.domain.user.friend.enums.FriendStatus;
import jakarta.validation.constraints.NotNull;

public record FriendStatusRequest(
    @NotNull(message = "변경할 상태값은 필수입니다.")
    FriendStatus status
) {

}
