package com.example.WonkaoTalk.domain.friend.dto;

import com.example.WonkaoTalk.domain.friend.enums.FriendStatus;
import jakarta.validation.constraints.NotNull;

public record FriendStatusRequest(
    @NotNull(message = "변경할 상태값은 필수입니다.")
    FriendStatus status
) {

}
