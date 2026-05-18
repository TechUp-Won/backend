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
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
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
public class ChatRoomService {

  private final ChatRoomRepo chatRoomRepo;
  private final ChatParticipantRepo chatParticipantRepo;
  private final UserRepo userRepo;
  private final ChatMessageRepo chatMessageRepo;

  @Transactional
  public ChatRoomResponse createChatRoom(Long userId, ChatRoomCreateRequest request) {
    Long receiverId = request.receiverId();

    if (userId.equals(receiverId)) {
      throw new BusinessException(ErrorCode.CANNOT_CHAT_SELF);
    }

    // TODO: 그룹 채팅 때는 findAllById를 사용해 한 번에 조회하도록 리팩토링 필요
    User me = userRepo.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    User receiver = userRepo.findById(receiverId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    List<ChatRoomResponse.ParticipantDto> participants = List.of(
        ChatRoomResponse.ParticipantDto.builder()
            .userId(userId)
            .nickname(me.getNickname())
            .build(),
        ChatRoomResponse.ParticipantDto.builder()
            .userId(receiverId)
            .nickname(receiver.getNickname())
            .build()
    );

    return chatParticipantRepo.findChatRoomByUsers(userId, receiverId)
        .map(room -> ChatRoomResponse.from(room, participants))
        .orElseGet(() -> {
          ChatRoom newRoom = chatRoomRepo.save(
              ChatRoom.builder()
                  .roomType(RoomType.SINGLE)
                  .participantCount(2)
                  .build()
          );

          chatParticipantRepo.save(
              ChatParticipant.builder()
                  .chatRoom(newRoom)
                  .userId(userId)
                  .roomTitle(receiver.getNickname())
                  .roomImage(receiver.getImage())
                  .build());

          chatParticipantRepo.save(
              ChatParticipant.builder()
                  .chatRoom(newRoom)
                  .userId(receiverId)
                  .roomTitle(me.getNickname())
                  .roomImage(me.getImage())
                  .build());

          return ChatRoomResponse.from(newRoom, participants);
        });
  }

  public ChatRoomListResponse getChatRoomList(Long userId, LocalDateTime lastMessageAt,
      Long cursorId,
      int size) {
    PageRequest pageRequest = PageRequest.of(0, size);

    Slice<ChatParticipant> slice = chatParticipantRepo.findMyChatRooms(userId, lastMessageAt,
        cursorId, pageRequest);

    List<ChatRoomInfo> rooms = slice.getContent().stream()
        .map(participant -> {
          Long lastReadMessageId = null;
          if (participant.getLastReadMessage() != null) {
            lastReadMessageId = participant.getLastReadMessage().getId();
          }

          // TODO: 목록 크기만큼 쿼리가 발생 추후 성능 최적화 방향 고려
          int unreadCount = chatMessageRepo.countUnreadMessages(
              participant.getChatRoom().getId(),
              lastReadMessageId
          );

          return ChatRoomInfo.from(participant, unreadCount);
        })
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
}
