package com.example.WonkaoTalk.domain.chat.dto;

import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ChatMessageResponse(
    Long messageId,
    Long senderId,
    String nickname,
    String content,
    MessageType messageType,
    LocalDateTime createdAt,
    Integer unreadCount
) {

  public static ChatMessageResponse of(ChatMessage chatMessage, String senderNickname,
      Integer unreadCount) {
    return ChatMessageResponse.builder()
        .messageId(chatMessage.getId())
        .senderId(chatMessage.getSenderId())
        .nickname(senderNickname)
        .content(chatMessage.getContent())
        .messageType(chatMessage.getMessageType())
        .createdAt(chatMessage.getCreatedAt())
        .unreadCount(unreadCount)
        .build();
  }
}
