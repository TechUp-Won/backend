package com.example.WonkaoTalk.domain.chat.service;

import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;

public interface ChatMessageService {

  ChatMessageResponse sendMessage(Long userId, Long chatRoomId, ChatMessageRequest request);
}
