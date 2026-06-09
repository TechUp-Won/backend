package com.example.WonkaoTalk.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;
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
class ChatRoomServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long RECEIVER_ID = 2L;
  private static final Long ROOM_ID = 100L;

  @InjectMocks
  private ChatRoomService chatRoomService;

  @Mock
  private ChatRoomRepo chatRoomRepo;

  @Mock
  private ChatParticipantRepo chatParticipantRepo;

  @Mock
  private UserRepo userRepo;

  @Mock
  private ChatMessageRepo chatMessageRepo;

  // 채팅방 생성
  @Test
  @DisplayName("채팅방 생성 성공 - 기존 방 존재 시 조회")
  void createChatRoomWhenRoomExists_ReturnsExistingRoom() {
    // given
    User me = createUser(USER_ID, "나");
    User receiver = createUser(RECEIVER_ID, "상대방");
    ChatRoom room = createRoom();

    given(userRepo.findById(USER_ID)).willReturn(Optional.of(me));
    given(userRepo.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));
    given(chatParticipantRepo.findChatRoomByUsers(USER_ID, RECEIVER_ID)).willReturn(
        Optional.of(room));

    // when
    ChatRoomResponse result = chatRoomService.createChatRoom(USER_ID,
        new ChatRoomCreateRequest(RECEIVER_ID));

    // then
    assertThat(result.chatRoomId()).isEqualTo(ROOM_ID);
    assertThat(result.participants()).hasSize(2);
    verify(chatRoomRepo, never()).save(any());
  }

  @Test
  @DisplayName("채팅방 생성 성공 - 새 방 생성")
  void createChatRoomWhenNewRoomReturnsCreatedRoom() {
    // given
    User me = createUser(USER_ID, "나");
    User receiver = createUser(RECEIVER_ID, "상대방");
    ChatRoom room = createRoom();

    given(userRepo.findById(USER_ID)).willReturn(Optional.of(me));
    given(userRepo.findById(RECEIVER_ID)).willReturn(Optional.of(receiver));
    given(chatParticipantRepo.findChatRoomByUsers(USER_ID, RECEIVER_ID)).willReturn(
        Optional.empty());
    given(chatRoomRepo.save(any(ChatRoom.class))).willReturn(room);

    // when
    ChatRoomResponse result = chatRoomService.createChatRoom(USER_ID,
        new ChatRoomCreateRequest(RECEIVER_ID));

    // then
    assertThat(result.chatRoomId()).isEqualTo(ROOM_ID);
    assertThat(result.roomType()).isEqualTo(RoomType.SINGLE);
    verify(chatRoomRepo, times(1)).save(any(ChatRoom.class));
    verify(chatParticipantRepo, times(2)).save(any(ChatParticipant.class));
  }

  @Test
  @DisplayName("자기 자신과 채팅방 생성 시 예외 발생")
  void createChatRoomWhenSelfThrowsException() {
    // when & then
    assertThatThrownBy(
        () -> chatRoomService.createChatRoom(USER_ID, new ChatRoomCreateRequest(USER_ID)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CANNOT_CHAT_SELF);
  }

  @Test
  @DisplayName("내 유저 정보가 없으면 예외 발생")
  void createChatRoomWhenMyUserNotFoundThrowsException() {
    // given
    given(userRepo.findById(USER_ID)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
        () -> chatRoomService.createChatRoom(USER_ID, new ChatRoomCreateRequest(RECEIVER_ID)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("상대 유저 정보가 없으면 예외 발생")
  void createChatRoomWhenReceiverNotFoundThrowsException() {
    // given
    User me = createUser(USER_ID, "나");

    given(userRepo.findById(USER_ID)).willReturn(Optional.of(me));
    given(userRepo.findById(RECEIVER_ID)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
        () -> chatRoomService.createChatRoom(USER_ID, new ChatRoomCreateRequest(RECEIVER_ID)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  // 채팅방 목록 조회
  @Test
  @DisplayName("채팅방 목록 조회 성공")
  void getChatRoomListWhenRoomsExistReturnsList() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatParticipantRepo.findMyChatRooms(USER_ID, null, null, PageRequest.of(0, 20)))
        .willReturn(createSlice(true, participant));
    given(chatMessageRepo.countUnreadMessages(ROOM_ID, null))
        .willReturn(3);

    // when
    ChatRoomListResponse result = chatRoomService.getChatRoomList(USER_ID, null, null, 20);

    // then
    assertThat(result.rooms()).hasSize(1);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursorId()).isEqualTo(ROOM_ID);
    assertThat(result.rooms().getFirst().unreadCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("마지막 페이지 조회 시 nextLastMessageAt은 null")
  void getChatRoomListLastPageReturnsNullCursors() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatParticipantRepo.findMyChatRooms(USER_ID, null, null, PageRequest.of(0, 20)))
        .willReturn(createSlice(false, participant));
    given(chatMessageRepo.countUnreadMessages(eq(ROOM_ID), any())).willReturn(0);

    // when
    ChatRoomListResponse result = chatRoomService.getChatRoomList(USER_ID, null, null, 20);

    // then
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursorId()).isNull();
    assertThat(result.nextLastMessageAt()).isNull();
  }

  @Test
  @DisplayName("다음 페이지가 존재하면 nextLastMessageAt 반환")
  void getChatRoomListReturnsNextLastMessageAt() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(
        chatParticipantRepo.findMyChatRooms(USER_ID, null, null, PageRequest.of(0, 20))).willReturn(
        createSlice(true, participant));

    given(chatMessageRepo.countUnreadMessages(ROOM_ID, null)).willReturn(0);

    // when
    ChatRoomListResponse result = chatRoomService.getChatRoomList(USER_ID, null, null, 20);

    // then
    assertThat(result.nextLastMessageAt()).isEqualTo(room.getLastMessageAt());
  }

  @Test
  @DisplayName("마지막 읽은 메시지가 없을 때 전체 안읽은 수 계산")
  void getChatRoomListWhenLastReadMessageIsNullUnreadCount() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);

    given(chatParticipantRepo.findMyChatRooms(USER_ID, null, null, PageRequest.of(0, 20)))
        .willReturn(createSlice(false, participant));
    given(chatMessageRepo.countUnreadMessages(ROOM_ID, null)).willReturn(5);

    // when
    ChatRoomListResponse result = chatRoomService.getChatRoomList(USER_ID, null, null, 20);

    // then
    verify(chatMessageRepo).countUnreadMessages(ROOM_ID, null);
    assertThat(result.rooms().getFirst().unreadCount()).isEqualTo(5);
  }

  @Test
  @DisplayName("읽은 메시지 정보가 있을 때 읽지 않은 메시지 개수 계산 성공")
  void getChatRoomListWithLastReadMessageReturnsUnreadCount() {
    // given
    ChatRoom room = createRoom();
    ChatParticipant participant = createParticipant(room);
    ChatMessage lastRead = createMessage(room);
    participant.updateLastReadMessage(lastRead);

    given(chatParticipantRepo.findMyChatRooms(USER_ID, null, null, PageRequest.of(0, 20)))
        .willReturn(createSlice(false, participant));
    given(chatMessageRepo.countUnreadMessages(ROOM_ID, 10L))
        .willReturn(1);

    // when
    ChatRoomListResponse result = chatRoomService.getChatRoomList(USER_ID, null, null, 20);

    // then
    assertThat(result.rooms().getFirst().unreadCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("채팅방이 없으면 빈 목록 반환")
  void getChatRoomListWhenEmptyReturnsEmptyList() {
    // given
    given(chatParticipantRepo.findMyChatRooms(USER_ID, null, null, PageRequest.of(0, 20)))
        .willReturn(createSlice(false));

    // when
    ChatRoomListResponse result = chatRoomService.getChatRoomList(USER_ID, null, null, 20);

    // then
    assertThat(result.rooms()).isEmpty();
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursorId()).isNull();
  }

  @Test
  @DisplayName("채팅방 수만큼 unread 조회")
  void getChatRoomListCallsUnreadCountPerRoom() {

    // given
    ChatRoom room1 = createRoom();
    ChatRoom room2 = ChatRoom.builder().roomType(RoomType.SINGLE).participantCount(2).build();
    ReflectionTestUtils.setField(room2, "id", 200L);
    ChatParticipant participant1 = createParticipant(room1);

    ChatParticipant participant2 = ChatParticipant.builder().chatRoom(room2).userId(USER_ID)
        .roomTitle("상대방2").roomImage("profile2.png").build();

    ReflectionTestUtils.setField(participant2, "id", 20L);

    given(
        chatParticipantRepo.findMyChatRooms(USER_ID, null, null, PageRequest.of(0, 20))).willReturn(
        createSlice(false, participant1, participant2));

    given(chatMessageRepo.countUnreadMessages(any(), any())).willReturn(0);

    // when
    chatRoomService.getChatRoomList(USER_ID, null, null, 20);

    // then
    verify(chatMessageRepo, times(2)).countUnreadMessages(any(), any());
  }

  // -- 헬퍼 메서드 --
  private User createUser(Long id, String nickname) {
    User user = User.builder()
        .nickname(nickname)
        .name(nickname)
        .phone("01012341234")
        .image("profile.png")
        .build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private ChatRoom createRoom() {
    ChatRoom room = ChatRoom.builder()
        .roomType(RoomType.SINGLE)
        .participantCount(2)
        .lastMessageContent("안녕")
        .build();
    ReflectionTestUtils.setField(room, "id", ROOM_ID);
    ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(room, "lastMessageAt", LocalDateTime.now());
    return room;
  }

  private ChatParticipant createParticipant(ChatRoom room) {
    ChatParticipant participant = ChatParticipant.builder()
        .chatRoom(room)
        .userId(USER_ID)
        .roomTitle("상대방")
        .roomImage("profile.png")
        .build();
    ReflectionTestUtils.setField(participant, "id", 10L);
    return participant;
  }

  private ChatMessage createMessage(ChatRoom room) {
    ChatMessage message = ChatMessage.builder()
        .chatRoom(room)
        .senderId(USER_ID)
        .content("안녕")
        .messageType(MessageType.TEXT)
        .build();
    ReflectionTestUtils.setField(message, "id", 10L);
    ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());
    return message;
  }

  private Slice<ChatParticipant> createSlice(boolean hasNext, ChatParticipant... participants) {
    return new SliceImpl<>(List.of(participants), PageRequest.of(0, 20), hasNext);
  }
}
