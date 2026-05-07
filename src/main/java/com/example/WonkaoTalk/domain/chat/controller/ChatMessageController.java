package com.example.WonkaoTalk.domain.chat.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;
import com.example.WonkaoTalk.domain.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatMessageController {

  private final ChatMessageService chatMessageService;

  @PostMapping("/{chatRoomId}/messages")
  public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
      @PathVariable Long chatRoomId,
      @Valid @RequestBody ChatMessageRequest request
      // @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    // TODO 연동 전 임시로 ID넣어둠
    Long myId = 1L;

    ChatMessageResponse data = chatMessageService.sendMessage(myId, chatRoomId, request);

    return ResponseEntity.ok(ApiResponse.success("메시지를 전송했습니다.", data));
  }

  @GetMapping("/{chatRoomId}/messages")
  public ResponseEntity<ApiResponse<ChatMessageListResponse>> getMessageList(
      @PathVariable Long chatRoomId,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "20") int size
      // @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    // TODO 연동 전 임시로 ID넣어둠
    Long myId = 1L;
    
    ChatMessageListResponse data = chatMessageService.getMessageList(myId, chatRoomId, cursorId,
        size);

    return ResponseEntity.ok(ApiResponse.success("메시지 내역을 불러왔습니다.", data));
  }
}
