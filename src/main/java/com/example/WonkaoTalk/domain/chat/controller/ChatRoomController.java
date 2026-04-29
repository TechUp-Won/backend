package com.example.WonkaoTalk.domain.chat.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;
import com.example.WonkaoTalk.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  @PostMapping
  public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
      @jakarta.validation.Valid @RequestBody ChatRoomCreateRequest request
      // @AuthenticationPrincipal CustomUserDetails userDetails
  ) { // TODO 연동 전 임시로 ID넣어둠
    Long myId = 1L;

    ChatRoomResponse data = chatRoomService.createChatRoom(myId, request);

    return ResponseEntity.ok(ApiResponse.success("채팅방이 생성되었습니다.", data));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<ChatRoomListResponse>> getChatRoomList(
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "20") int size
  ) {
    // TODO 연동 전 임시로 ID 넣어둠
    Long myId = 1L;

    ChatRoomListResponse data = chatRoomService.getChatRoomList(myId, cursorId, size);

    return ResponseEntity.ok(ApiResponse.success("채팅방 목록을 불러왔습니다.", data));
  }
}
