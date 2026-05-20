package com.example.WonkaoTalk.domain.chat.controller;

import com.example.WonkaoTalk.common.config.OpenApiConfig;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;
import com.example.WonkaoTalk.domain.chat.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chats")
@Tag(name = "채팅방", description = "채팅방 생성과 채팅방 목록 조회 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  @Operation(summary = "채팅방 생성", description = "상대 사용자와의 채팅방을 생성하거나 기존 채팅방 정보를 반환합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ChatRoomCreateRequest request
  ) {
    Long userId = userDetails.getUserId();

    ChatRoomResponse data = chatRoomService.createChatRoom(userId, request);

    return ResponseEntity.ok(ApiResponse.success("채팅방이 생성되었습니다.", data));
  }

  @Operation(summary = "채팅방 목록 조회", description = "로그인한 사용자의 채팅방 목록을 커서 기반으로 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<ChatRoomListResponse>> getChatRoomList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastMessageAt,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "20") int size
  ) {
    Long userId = userDetails.getUserId();
    
    ChatRoomListResponse data = chatRoomService.getChatRoomList(userId, lastMessageAt, cursorId,
        size);

    return ResponseEntity.ok(ApiResponse.success("채팅방 목록을 불러왔습니다.", data));
  }
}
