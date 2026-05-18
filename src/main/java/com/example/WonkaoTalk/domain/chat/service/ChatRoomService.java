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

  @Transactional
  public ChatRoomResponse createChatRoom(Long authId, ChatRoomCreateRequest request) {
    Long receiverAuthId = request.receiverId();

    if (authId.equals(receiverAuthId)) {
      throw new BusinessException(ErrorCode.CANNOT_CHAT_SELF);
    }

    User me = userRepo.findByAuthId(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    User receiver = userRepo.findByAuthId(receiverAuthId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    Long userId = me.getId();
    Long receiverId = receiver.getId();

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

  public ChatRoomListResponse getChatRoomList(Long authId, LocalDateTime lastMessageAt,
      Long cursorId,
      int size) {
    User me = userRepo.findByAuthId(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    Long userId = me.getId();

    PageRequest pageRequest = PageRequest.of(0, size);

    Slice<ChatParticipant> slice = chatParticipantRepo.findMyChatRooms(userId, lastMessageAt,
        cursorId, pageRequest);

    List<ChatRoomInfo> rooms = slice.getContent().stream()
        // TODO unreadCount 임시로 0 넣어 놓음
        .map(participant -> ChatRoomInfo.from(participant, 0))
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
