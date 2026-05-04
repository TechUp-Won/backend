package com.example.WonkaoTalk.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;
import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

  @InjectMocks
  private ChatMessageServiceImpl chatMessageService;

  @Mock
  private ChatMessageRepository chatMessageRepository;
  @Mock
  private ChatRoomRepository chatRoomRepository;
  @Mock
  private ChatParticipantRepository chatParticipantRepository;

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

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(room));
    when(chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
        .thenReturn(Optional.of(participant));

    when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
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

    verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
  }

  @Test
  @DisplayName("❌ 실패 - 존재하지 않는 채팅방")
  void sendMessageFailRoomNotFound() {
    // given
    Long userId = 1L;
    Long chatRoomId = 999L;
    ChatMessageRequest request = ChatMessageRequest.builder().content("테스트").build();

    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.empty());

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
    when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(room));

    when(chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId, userId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.sendMessage(userId, chatRoomId, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_CHAT_PARTICIPANT);
  }
}
