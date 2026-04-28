package com.example.WonkaoTalk.domain.chat.dto;

import lombok.Builder;

@Builder
public record ChatRoomCreateRequest(
    @jakarta.validation.constraints.NotNull(message = "상대방 ID는 필수입니다.")
    Long receiverId
) {

}
