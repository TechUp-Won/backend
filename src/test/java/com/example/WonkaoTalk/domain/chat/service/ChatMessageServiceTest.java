package com.example.WonkaoTalk.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;
import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

  @InjectMocks
  private ChatMessageService chatMessageService;

  @Mock
  private ChatMessageRepo chatMessageRepo;
  @Mock
  private ChatRoomRepo chatRoomRepo;
  @Mock
  private ChatParticipantRepo chatParticipantRepo;

  @Test
  @DisplayName("✅ 메시지 전송 성공")
  void sendMessageSuccessTest() {
    // given
    Long userId = 1L;
    Long chatRoomId = 10L;
    ChatMessageRequest request = ChatMessageRequest.builder()
        .content("테스트")
        .messageType(MessageType.TEXT)
        .build();

    ChatRoom room = ChatRoom.builder().build();
    ChatParticipant participant = ChatParticipant.builder().build();

    when(chatRoomRepo.findById(chatRoomId)).thenReturn(Optional.of(room));
    when(chatParticipantRepo.findByChatRoomIdAndUserId(chatRoomId, userId))
        .thenReturn(Optional.of(participant));

    when(chatMessageRepo.saveAndFlush(any(ChatMessage.class))).thenAnswer(invocation -> {
      ChatMessage message = invocation.getArgument(0);
      ReflectionTestUtils.setField(message, "id", 100L);
      return message;
    });

    // when
    ChatMessageResponse response = chatMessageService.sendMessage(userId, chatRoomId, request);

    // then
    assertThat(response.messageId()).isEqualTo(100L);

    assertThat(room.getLastMessageContent()).isEqualTo("테스트");
    assertThat(participant.getLastReadMessage()).isNotNull();

    verify(chatMessageRepo, times(1)).saveAndFlush(any(ChatMessage.class));
  }

  @Test
  @DisplayName("❌ 실패 - 존재하지 않는 채팅방")
  void sendMessageFailRoomNotFound() {
    // given
    Long userId = 1L;
    Long chatRoomId = 999L;
    ChatMessageRequest request = ChatMessageRequest.builder().content("테스트").build();

    when(chatRoomRepo.findById(chatRoomId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.sendMessage(userId, chatRoomId, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("❌ 실패 - 채팅방 참여 권한 없음")
  void sendMessageFailNotParticipant() {
    // given
    Long userId = 1L;
    Long chatRoomId = 10L;
    ChatMessageRequest request = ChatMessageRequest.builder().content("테스트").build();

    ChatRoom room = ChatRoom.builder().build();
    when(chatRoomRepo.findById(chatRoomId)).thenReturn(Optional.of(room));

    when(chatParticipantRepo.findByChatRoomIdAndUserId(chatRoomId, userId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.sendMessage(userId, chatRoomId, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_CHAT_PARTICIPANT);
  }

  @Test
  @DisplayName("✅ 메시지 내역 조회 성공")
  void getMessageListSuccessNoCursorTest() {
    // given
    Long myId = 1L;
    Long chatRoomId = 10L;
    int size = 20;

    ChatRoom chatRoom = createChatRoom(chatRoomId);
    ChatParticipant myParticipant = createParticipant(chatRoom, myId);

    ChatMessage msg1 = createMessage(200L, chatRoom, 2L, "상대방이 보낸 최신 메시지");
    ChatMessage msg2 = createMessage(199L, chatRoom, myId, "내가 보낸 과거 메시지");
    List<ChatMessage> mockMessages = List.of(msg1, msg2);

    when(chatParticipantRepo.findByChatRoomIdAndUserId(chatRoomId, myId))
        .thenReturn(Optional.of(myParticipant));

    when(chatMessageRepo.findMessagesByCursor(eq(chatRoomId), eq(null),
        any(PageRequest.class)))
        .thenReturn(new SliceImpl<>(mockMessages, PageRequest.of(0, size), true));

    when(chatParticipantRepo.findOtherParticipantsLastReadMessageIds(chatRoomId, myId))
        .thenReturn(List.of(200L));

    // when
    ChatMessageListResponse response = chatMessageService.getMessageList(myId, chatRoomId, null,
        size);

    // then
    assertThat(response).isNotNull();
    assertThat(response.messageList()).hasSize(2);
    assertThat(response.nextCursorId()).isEqualTo(199L);

    assertThat(response.messageList().get(0).isMe()).isFalse();
    assertThat(response.messageList().get(0).unreadCount()).isEqualTo(0);

    assertThat(response.messageList().get(1).isMe()).isTrue();
    assertThat(response.messageList().get(1).unreadCount()).isEqualTo(0);

    assertThat(myParticipant.getLastReadMessage().getId()).isEqualTo(200L);
  }

  private ChatRoom createChatRoom(Long id) {
    ChatRoom room = ChatRoom.builder().roomType(RoomType.SINGLE).build();
    ReflectionTestUtils.setField(room, "id", id);
    return room;
  }

  private ChatParticipant createParticipant(ChatRoom room, Long userId) {
    ChatParticipant participant = ChatParticipant.builder().chatRoom(room).userId(userId).build();
    ReflectionTestUtils.setField(participant, "id", 1L);
    return participant;
  }

  private ChatMessage createMessage(Long id, ChatRoom room, Long senderId, String content) {
    ChatMessage message = ChatMessage.builder()
        .chatRoom(room)
        .senderId(senderId)
        .content(content)
        .messageType(MessageType.TEXT)
        .build();
    ReflectionTestUtils.setField(message, "id", id);
    ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());
    return message;
  }
}
