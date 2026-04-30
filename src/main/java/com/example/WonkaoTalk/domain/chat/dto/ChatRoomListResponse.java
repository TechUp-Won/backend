package com.example.WonkaoTalk.domain.chat.dto;

import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record ChatRoomListResponse(
    List<ChatRoomInfo> rooms,
    Boolean hasNext,
    Long nextCursorId,
    LocalDateTime nextLastMessageAt
) {

  @Builder
  public record ChatRoomInfo(
      Long chatRoomId,
      RoomType roomType,
      String roomTitle,
      String roomImage,
      String lastMessageContent,
      LocalDateTime lastMessageAt,
      Integer unreadCount,
      Integer participantCount
  ) {

    public static ChatRoomInfo from(ChatRoom room, Integer unreadCount) {
      return ChatRoomInfo.builder()
          .chatRoomId(room.getId())
          .roomType(room.getRoomType())
          .roomTitle(room.getTitle())
          .roomImage(room.getRoomImage())
          .lastMessageContent(room.getLastMessageContent())
          .lastMessageAt(room.getLastMessageAt())
          .unreadCount(unreadCount)
          .participantCount(room.getParticipantCount())
          .build();
    }
  }
}
