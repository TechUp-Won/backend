package com.example.WonkaoTalk.domain.chat.dto;

import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import lombok.Builder;

@Builder
public record ChatMessageResponse(
    Long messageId
) {

  public static ChatMessageResponse from(ChatMessage chatMessage) {
    return ChatMessageResponse.builder()
        .messageId(chatMessage.getId())
        .build();
  }
}
