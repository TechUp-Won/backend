package com.example.WonkaoTalk.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long ROOM_ID = 100L;

  @InjectMocks
  private ChatMessageService chatMessageService;

  @Mock
  private ChatMessageRepo chatMessageRepo;

  @Mock
  private ChatRoomRepo chatRoomRepo;

  @Mock
  private ChatParticipantRepo chatParticipantRepo;

  @Mock
  private UserRepo userRepo;

  // 구분용: 메시지전송
  @Test
  @DisplayName("메시지 전송 성공")
  void sendMessageSuccess() {
    // given
    User user = createUser(USER_ID, "나");
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.of(room));
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    given(userRepo.findById(USER_ID)).willReturn(Optional.of(user));

    givenUnreadIds(2L, null);
    givenSaveMessage();

    // when
    ChatMessageResponse result = chatMessageService.sendMessage(USER_ID, ROOM_ID, request());

    // then
    assertThat(result.content()).isEqualTo("안녕");
    assertThat(result.nickname()).isEqualTo("나");
    assertThat(result.unreadCount()).isEqualTo(1);
    assertThat(room.getLastMessageContent()).isEqualTo("안녕");
    assertThat(participant.getLastReadMessage()).isNotNull();

    verify(chatMessageRepo).saveAndFlush(any(ChatMessage.class));
  }

  @Test
  @DisplayName("답장 메시지 전송 성공")
  void sendMessageAnswerSuccess() {
    // given
    User user = createUser(USER_ID, "나");
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);
    ChatMessage answer = createMessage(room, 10L, USER_ID, "원본");

    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.of(room));
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    given(userRepo.findById(USER_ID)).willReturn(Optional.of(user));
    given(chatMessageRepo.findById(10L)).willReturn(Optional.of(answer));

    givenUnreadIds(1L);
    givenSaveMessage();

    // when
    ChatMessageResponse result = chatMessageService.sendMessage(
        USER_ID,
        ROOM_ID,
        new ChatMessageRequest("답장", MessageType.TEXT, 10L)
    );

    // then
    assertThat(result.content()).isEqualTo("답장");
  }

  @Test
  @DisplayName("모든 참여자가 읽은 상태면 unreadCount 0")
  void sendMessageAllReadUnreadCountZero() {
    // given
    User user = createUser(USER_ID, "나");
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.of(room));
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    given(userRepo.findById(USER_ID)).willReturn(Optional.of(user));

    givenSaveMessage(); // 반환되는 메시지 ID는 2L로 세팅됨
    // 두 명의 참여자가 모두 최신 메시지(2L) 이상을 읽었다고 가정
    givenUnreadIds(2L, 5L);

    // when
    ChatMessageResponse result = chatMessageService.sendMessage(USER_ID, ROOM_ID, request());

    // then
    assertThat(result.unreadCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("읽지 않은 참여자 존재 시 unreadCount 증가")
  void sendMessageUnreadCountCalculated() {
    // given
    User user = createUser(USER_ID, "나");
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.of(room));
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    given(userRepo.findById(USER_ID)).willReturn(Optional.of(user));
    givenSaveMessage();
    givenUnreadIds(1L, null);

    // when
    ChatMessageResponse result = chatMessageService.sendMessage(USER_ID, ROOM_ID, request());

    // then
    assertThat(result.unreadCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("채팅방이 없으면 예외 발생")
  void sendMessageRoomNotFoundFail() {
    // given
    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.sendMessage(USER_ID, ROOM_ID, request()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("채팅 참여자가 아니면 예외 발생")
  void sendMessageNotParticipantFail() {
    // given
    ChatRoom room = createRoom();

    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.of(room));
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.sendMessage(USER_ID, ROOM_ID, request()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_CHAT_PARTICIPANT);
  }

  @Test
  @DisplayName("유저가 없으면 예외 발생")
  void sendMessageUserNotFoundFail() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.of(room));
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    given(userRepo.findById(USER_ID)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.sendMessage(USER_ID, ROOM_ID, request()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("답장 메시지가 없으면 예외 발생")
  void sendMessageAnswerMessageNotFoundFail() {
    // given
    User user = createUser(USER_ID, "나");
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatRoomRepo.findById(ROOM_ID)).willReturn(Optional.of(room));
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    given(userRepo.findById(USER_ID)).willReturn(Optional.of(user));
    given(chatMessageRepo.findById(99L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.sendMessage(USER_ID, ROOM_ID,
        new ChatMessageRequest("답장", MessageType.TEXT, 99L)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
  }

  // 구분용: 목록조회쪽
  @Test
  @DisplayName("메시지 목록 조회 성공")
  void getMessageListSuccess() {
    // given
    User user = createUser(USER_ID, "나");
    User otherUser = createUser(2L, "너");
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    ChatMessage message1 = createMessage(room, 10L, USER_ID, "안녕");
    ChatMessage message2 = createMessage(room, 9L, 2L, "반가워");

    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    givenMessageSlice(true, message1, message2);
    givenUnreadIds(9L);
    given(userRepo.findAllById(any())).willReturn(List.of(user, otherUser));

    // when
    ChatMessageListResponse result = chatMessageService.getMessageList(USER_ID, ROOM_ID, null, 20);

    // then
    assertThat(result.messageList()).hasSize(2);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursorId()).isEqualTo(9L);
    assertThat(participant.getLastReadMessage().getId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("닉네임이 없으면 알 수 없음 처리")
  void getMessageListUnknownNicknameSuccess() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);
    ChatMessage message = createMessage(room, 10L, 999L, "hello");

    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    givenMessageSlice(false, message);
    givenUnreadIds();
    given(userRepo.findAllById(any())).willReturn(List.of());

    // when
    ChatMessageListResponse result = chatMessageService.getMessageList(USER_ID, ROOM_ID, null, 20);

    // then
    assertThat(result.messageList().getFirst().nickname()).isEqualTo("(알 수 없음)");
  }

  @Test
  @DisplayName("메시지가 없으면 빈 목록 반환")
  void getMessageListEmptySuccess() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    givenMessageSlice(false);
    givenUnreadIds();
    given(userRepo.findAllById(any())).willReturn(List.of());

    // when
    ChatMessageListResponse result = chatMessageService.getMessageList(USER_ID, ROOM_ID, null, 20);

    // then
    assertThat(result.messageList()).isEmpty();
    assertThat(result.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("마지막 페이지 조회 시 반환 잘 되는지")
  void getMessageListLastPageReturnsNullCursor() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);
    ChatMessage message = createMessage(room, 5L, USER_ID, "마지막 메시지");

    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    givenMessageSlice(false, message);
    givenUnreadIds();
    given(userRepo.findAllById(any())).willReturn(List.of());

    // when
    ChatMessageListResponse result = chatMessageService.getMessageList(USER_ID, ROOM_ID, null, 20);

    // then
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("기존 읽음 메시지가 최신이면 updateLastReadMessage 미호출")
  void getMessageListKeepLastReadMessageWhenAlreadyLatest() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);
    ChatMessage alreadyReadMessage = createMessage(room, 20L, USER_ID, "읽은 메시지");
    participant.updateLastReadMessage(alreadyReadMessage);
    ChatMessage fetchedMessage = createMessage(room, 10L, 2L, "과거 메시지");

    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.of(participant));
    givenMessageSlice(true, fetchedMessage);
    givenUnreadIds();
    given(userRepo.findAllById(any())).willReturn(List.of());

    // when
    chatMessageService.getMessageList(USER_ID, ROOM_ID, null, 20);

    // then
    assertThat(participant.getLastReadMessage().getId()).isEqualTo(20L);
  }

  @Test
  @DisplayName("참여자가 아니면 메시지 조회 실패")
  void getMessageListNotParticipantFail() {
    // given
    given(chatParticipantRepo.findByChatRoomIdAndUserId(ROOM_ID, USER_ID)).willReturn(
        Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatMessageService.getMessageList(USER_ID, ROOM_ID, null, 20))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_CHAT_PARTICIPANT);
  }

  // -- 헬퍼 메서드 --
  private void givenUnreadIds(Long... ids) {
    given(chatParticipantRepo.findAllParticipantsLastReadMessageIds(ROOM_ID))
        .willReturn(Arrays.asList(ids));
  }

  private void givenSaveMessage() {
    given(chatMessageRepo.saveAndFlush(any(ChatMessage.class))).willAnswer(invocation -> {
      ChatMessage message = invocation.getArgument(0);
      ReflectionTestUtils.setField(message, "id", 2L);
      ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());
      return message;
    });
  }

  private void givenMessageSlice(boolean hasNext, ChatMessage... messages) {
    Slice<ChatMessage> slice = new SliceImpl<>(List.of(messages), PageRequest.of(0, 20), hasNext);
    given(chatMessageRepo.findMessagesByCursor(ROOM_ID, null, PageRequest.of(0, 20)))
        .willReturn(slice);
  }

  private User createUser(Long id, String nickname) {
    User user = User.builder()
        .nickname(nickname)
        .name(nickname)
        .phone("01012341234")
        .build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private ChatRoom createRoom() {
    ChatRoom room = ChatRoom.builder()
        .roomType(RoomType.SINGLE)
        .participantCount(2)
        .build();
    ReflectionTestUtils.setField(room, "id", ROOM_ID);
    return room;
  }

  private ChatParticipant createParticipant(ChatRoom room) {
    ChatParticipant participant = ChatParticipant.builder()
        .chatRoom(room)
        .userId(USER_ID)
        .build();
    ReflectionTestUtils.setField(participant, "id", 10L);
    return participant;
  }

  private ChatMessage createMessage(ChatRoom room, Long id, Long senderId, String content) {
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

  private ChatMessageRequest request() {
    return new ChatMessageRequest("안녕", MessageType.TEXT, null);
  }
}
