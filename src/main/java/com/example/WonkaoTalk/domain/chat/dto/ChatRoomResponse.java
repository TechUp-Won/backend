package com.example.WonkaoTalk.domain.chat.dto;

import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import java.util.List;
import lombok.Builder;

@Builder
public record ChatRoomResponse(
    Long chatRoomId,
    RoomType roomType,
    List<ParticipantDto> participants,
    String createdAt
) {

  public static ChatRoomResponse from(ChatRoom room, List<ParticipantDto> participants) {
    return ChatRoomResponse.builder()
        .chatRoomId(room.getId())
        .roomType(room.getRoomType())
        .participants(participants)
        .createdAt(room.getCreatedAt().toString())
        .build();
  }

  @Builder
  public record ParticipantDto(Long userId, String nickname, String profileImage) {

  }
}
