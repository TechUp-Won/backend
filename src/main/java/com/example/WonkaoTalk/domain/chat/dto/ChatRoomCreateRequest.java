package com.example.WonkaoTalk.domain.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ChatRoomCreateRequest(
    @NotNull(message = "상대방 ID는 필수입니다.")
    Long receiverId
) {

}
