package com.example.WonkaoTalk.domain.chat.dto;

import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record ChatMessageListResponse(
    List<ChatMessageDto> messageList,
    Boolean hasNext,
    Long nextCursorId
) {

  public static ChatMessageListResponse of(List<ChatMessageDto> messageList, Boolean hasNext,
      Long nextCursorId) {
    return ChatMessageListResponse.builder()
        .messageList(messageList)
        .hasNext(hasNext)
        .nextCursorId(nextCursorId)
        .build();
  }

  @Builder
  public record ChatMessageDto(
      Long messageId,
      Long senderId,
      String nickname,
      Boolean isMe,
      String content,
      MessageType messageType,
      LocalDateTime createdAt,
      Integer unreadCount
  ) {

    public static ChatMessageDto of(ChatMessage message, Long myId, String senderNickname,
        Integer unreadCount) {
      boolean isMe = message.getSenderId() != null && message.getSenderId().equals(myId);

      return ChatMessageDto.builder()
          .messageId(message.getId())
          .senderId(message.getSenderId())
          .nickname(senderNickname)
          .isMe(isMe)
          .content(message.getContent())
          .messageType(message.getMessageType())
          .createdAt(message.getCreatedAt())
          .unreadCount(unreadCount)
          .build();
    }
  }
}
