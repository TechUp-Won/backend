package com.example.WonkaoTalk.domain.chat.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.integration.BaseIntegrationTest;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ChatApiE2ETest extends BaseIntegrationTest {

  @Autowired
  private AuthRepo authRepo;

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private User userA;
  private User userB;
  private User userC;

  private String tokenA;
  private String tokenB;
  private String tokenC;

  @BeforeEach
  void setUp() {

    Auth authA = authRepo.save(Auth.builder().build());
    Auth authB = authRepo.save(Auth.builder().build());
    Auth authC = authRepo.save(Auth.builder().build());

    userA = userRepo.save(
        User.builder()
            .auth(authA)
            .nickname("userA")
            .name("userA")
            .phone("010-1111-1111")
            .build()
    );

    userB = userRepo.save(
        User.builder()
            .auth(authB)
            .nickname("userB")
            .name("userB")
            .phone("010-2222-2222")
            .build()
    );

    userC = userRepo.save(
        User.builder()
            .auth(authC)
            .nickname("userC")
            .name("userC")
            .phone("010-3333-3333")
            .build()
    );

    tokenA = createToken(authA.getId(), userA.getId());
    tokenB = createToken(authB.getId(), userB.getId());
    tokenC = createToken(authC.getId(), userC.getId());
  }

  private String createToken(Long authId, Long userId) {
    return "Bearer " + jwtTokenProvider.createAccessToken(
        "test@test.com",
        authId,
        userId,
        null,
        "USER"
    );
  }

  private Long createRoom() throws Exception {

    ChatRoomCreateRequest request =
        ChatRoomCreateRequest.builder()
            .receiverId(userB.getId())
            .build();

    String response = mockMvc.perform(post("/api/v1/chats")
            .header("Authorization", tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andReturn()
        .getResponse()
        .getContentAsString();

    return objectMapper.readTree(response)
        .path("data")
        .path("chatRoomId")
        .asLong();
  }

  @Test
  @DisplayName("채팅방 생성 성공")
  void createChatRoomSuccess() throws Exception {

    ChatRoomCreateRequest request =
        ChatRoomCreateRequest.builder()
            .receiverId(userB.getId())
            .build();

    mockMvc.perform(post("/api/v1/chats")
            .header("Authorization", tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  @Test
  @DisplayName("자기 자신 채팅 예외")
  void createChatRoomSelfFail() throws Exception {

    ChatRoomCreateRequest request =
        ChatRoomCreateRequest.builder()
            .receiverId(userA.getId())
            .build();

    mockMvc.perform(post("/api/v1/chats")
            .header("Authorization", tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("CHAT-INVALID-SELF"));
  }

  @Test
  @DisplayName("존재하지 않는 유저 예외")
  void createChatRoomUserNotFound() throws Exception {

    ChatRoomCreateRequest request =
        ChatRoomCreateRequest.builder()
            .receiverId(999999L)
            .build();

    mockMvc.perform(post("/api/v1/chats")
            .header("Authorization", tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("USER-NOT-FOUND-ID"));
  }

  @Test
  @DisplayName("메시지 전송 성공")
  void sendMessageSuccess() throws Exception {

    Long roomId = createRoom();

    ChatMessageRequest request =
        ChatMessageRequest.builder()
            .content("안녕하세요")
            .messageType(MessageType.TEXT)
            .build();

    mockMvc.perform(post("/api/v1/chats/{roomId}/messages", roomId)
            .header("Authorization", tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  @Test
  @DisplayName("미참여자 메시지 전송 실패")
  void sendMessageNotParticipant() throws Exception {

    Long roomId = createRoom();

    ChatMessageRequest request =
        ChatMessageRequest.builder()
            .content("침입")
            .messageType(MessageType.TEXT)
            .build();

    mockMvc.perform(post("/api/v1/chats/{roomId}/messages", roomId)
            .header("Authorization", tokenC)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error")
            .value("CHAT-FORBIDDEN-PARTICIPANT"));
  }

  @Test
  @DisplayName("존재하지 않는 방 전송 실패")
  void sendMessageRoomNotFound() throws Exception {

    ChatMessageRequest request =
        ChatMessageRequest.builder()
            .content("안녕")
            .messageType(MessageType.TEXT)
            .build();

    mockMvc.perform(post("/api/v1/chats/99999/messages")
            .header("Authorization", tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error")
            .value("CHAT-NOT-FOUND-ROOM"));
  }

  @Test
  @DisplayName("빈 메시지 실패")
  void sendBlankMessageFail() throws Exception {

    Long roomId = createRoom();

    ChatMessageRequest request =
        ChatMessageRequest.builder()
            .content("")
            .messageType(MessageType.TEXT)
            .build();

    mockMvc.perform(post("/api/v1/chats/{roomId}/messages", roomId)
            .header("Authorization", tokenA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("SYS-INVALID-INPUT"));
  }

  @Test
  @DisplayName("메시지 조회 성공")
  void getMessageListSuccess() throws Exception {

    Long roomId = createRoom();

    mockMvc.perform(get("/api/v1/chats/{roomId}/messages", roomId)
            .header("Authorization", tokenB))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }

  @Test
  @DisplayName("미참여자 조회 실패")
  void getMessageListNotParticipant() throws Exception {

    Long roomId = createRoom();

    mockMvc.perform(get("/api/v1/chats/{roomId}/messages", roomId)
            .header("Authorization", tokenC))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error")
            .value("CHAT-FORBIDDEN-PARTICIPANT"));
  }
}
