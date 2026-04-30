package com.example.WonkaoTalk.domain.chat.service;

import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;
import java.time.LocalDateTime;

public interface ChatRoomService {

  ChatRoomResponse createChatRoom(Long myId, ChatRoomCreateRequest request);

  ChatRoomListResponse getChatRoomList(Long myId, LocalDateTime lastMessageAt, Long cursorId,
      int size);
}
