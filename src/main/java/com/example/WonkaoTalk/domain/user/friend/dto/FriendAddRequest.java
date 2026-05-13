package com.example.WonkaoTalk.domain.user.friend.dto;

import jakarta.validation.constraints.NotNull;

public record FriendAddRequest(
    @NotNull(message = "추가할 대상의 ID는 필수입니다.")
    Long targetId
) {

}
