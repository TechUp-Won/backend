package com.example.WonkaoTalk.domain.chat.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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

  public ChatRoomListResponse getChatRoomList(Long myId, Long cursorId, int size) {
    PageRequest pageRequest = PageRequest.of(0, size);

    Slice<ChatRoom> roomSlice = chatRoomRepository.findMyChatRooms(myId, cursorId, pageRequest);

    List<ChatRoomListResponse.ChatRoomInfo> roomSummaries = roomSlice.getContent().stream()
        .map(room -> {
          // TODO: 추후 last_read_message_id 기준으로 count 쿼리 연동
          Integer unreadCount = 0;
          return ChatRoomListResponse.ChatRoomInfo.from(room, unreadCount);
        })
        .toList();

    Long nextCursorId = roomSummaries.isEmpty() ? null :
        roomSummaries.get(roomSummaries.size() - 1).chatRoomId();

    return ChatRoomListResponse.builder()
        .rooms(roomSummaries)
        .hasNext(roomSlice.hasNext())
        .nextCursorId(roomSlice.hasNext() ? nextCursorId : null)
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
