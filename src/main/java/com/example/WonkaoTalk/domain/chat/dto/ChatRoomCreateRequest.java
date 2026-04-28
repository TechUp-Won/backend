package com.example.WonkaoTalk.domain.chat.dto;

import lombok.Builder;

@Builder
public record ChatRoomCreateRequest(
    Long receiverId
) {

}
