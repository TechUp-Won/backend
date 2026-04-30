package com.example.WonkaoTalk.domain.chat.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomListResponse.ChatRoomInfo;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository chatParticipantRepository;

  @Override
  @Transactional
  public ChatRoomResponse createChatRoom(Long myId, ChatRoomCreateRequest request) {
    Long receiverId = request.receiverId();

    if (myId.equals(receiverId)) {
      throw new BusinessException(ErrorCode.CANNOT_CHAT_SELF);
    }

    return chatParticipantRepository.findChatRoomByUsers(myId, receiverId)
        .map(room -> ChatRoomResponse.from(room, createTempParticipants(myId, receiverId)))
        .orElseGet(() -> {
          ChatRoom newRoom = chatRoomRepository.save(
              ChatRoom.builder()
                  .roomType(RoomType.SINGLE)
                  .participantCount(2)
                  .build()
          );

          // 내 참여 정보
          chatParticipantRepository.save(
              ChatParticipant.builder()
                  .chatRoom(newRoom)
                  .userId(myId)
                  .roomTitle("상대방 닉네임") // TODO: UserService 연동 시 receiver nickname/profile 주입
                  .roomImage("상대방 이미지URL")
                  .build());

          // 상대방 참여 정보
          chatParticipantRepository.save(
              ChatParticipant.builder()
                  .chatRoom(newRoom)
                  .userId(receiverId)
                  .roomTitle("나의 닉네임") // TODO: UserService 연동 시 my nickname/profile 주입
                  .roomImage("나의 이미지URL")
                  .build());

          return ChatRoomResponse.from(newRoom, createTempParticipants(myId, receiverId));
        });
  }

  @Override
  public ChatRoomListResponse getChatRoomList(Long myId, LocalDateTime lastMessageAt, Long cursorId,
      int size) {
    PageRequest pageRequest = PageRequest.of(0, size);

    Slice<ChatRoom> slice = chatRoomRepository.findMyChatRooms(myId,
        lastMessageAt,
        cursorId,
        pageRequest);

    List<ChatRoomInfo> rooms = slice.getContent().stream()
        // TODO unreadCount 임시로 0 넣어 놓음
        .map(room -> ChatRoomInfo.from(room, 0))
        .toList();

    Long nextCursorId = null;
    LocalDateTime nextLastMessageAt = null;

    if (slice.hasNext() && !rooms.isEmpty()) {
      ChatRoomInfo lastRoom = rooms.getLast();
      nextCursorId = lastRoom.chatRoomId();
      nextLastMessageAt = lastRoom.lastMessageAt();
    }

    return ChatRoomListResponse.builder()
        .rooms(rooms)
        .hasNext(slice.hasNext())
        .nextCursorId(nextCursorId)
        .nextLastMessageAt(nextLastMessageAt)
        .build();
  }

  // TODO: 유저 연동 전 임시 데이터임
  private List<ChatRoomResponse.ParticipantDto> createTempParticipants(Long myId, Long receiverId) {
    return List.of(
        ChatRoomResponse.ParticipantDto.builder().userId(myId).nickname("나").build(),
        ChatRoomResponse.ParticipantDto.builder().userId(receiverId).nickname("상대방").build()
    );
  }
}
