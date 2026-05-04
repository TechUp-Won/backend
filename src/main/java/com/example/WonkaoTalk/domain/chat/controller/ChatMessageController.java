package com.example.WonkaoTalk.domain.chat.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;
import com.example.WonkaoTalk.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chats")
public class ChatMessageController {

  private final ChatMessageService chatMessageService;

  @PostMapping("/{chatRoomId}/messages")
  public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
      @PathVariable Long chatRoomId,
      @jakarta.validation.Valid @RequestBody ChatMessageRequest request
      // @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    // TODO 연동 전 임시로 ID넣어둠
    Long myId = 1L;

    ChatMessageResponse data = chatMessageService.sendMessage(myId, chatRoomId, request);

    return ResponseEntity.ok(ApiResponse.success("메시지를 전송했습니다.", data));
  }
}
