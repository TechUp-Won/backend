package com.example.WonkaoTalk.domain.chat.service;

import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;

public interface ChatRoomService {

  ChatRoomResponse createChatRoom(Long myId, ChatRoomCreateRequest request);
}
