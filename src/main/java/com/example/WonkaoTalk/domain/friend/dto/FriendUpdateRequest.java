package com.example.WonkaoTalk.domain.friend.dto;

import jakarta.validation.constraints.Size;

public record FriendUpdateRequest(
    @Size(max = 20, message = "별칭은 20자를 초과할 수 없습니다.")
    String alias,

    @Size(max = 200, message = "메모는 200자를 초과할 수 없습니다.")
    String memo
) {

}
