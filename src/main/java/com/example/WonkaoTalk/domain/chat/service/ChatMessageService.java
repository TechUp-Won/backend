package com.example.WonkaoTalk.domain.chat.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;
import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository chatParticipantRepository;
  
  @Transactional
  public ChatMessageResponse sendMessage(Long userId, Long chatRoomId, ChatMessageRequest request) {
    ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

    ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId,
            userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

    ChatMessage answerMessage = null;
    if (request.answerMessageId() != null) {
      answerMessage = chatMessageRepository.findById(request.answerMessageId())
          .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
    }

    ChatMessage chatMessage = ChatMessage.builder()
        .chatRoom(chatRoom)
        .senderId(userId)
        .content(request.content())
        .messageType(request.messageType())
        .answerMessage(answerMessage)
        .build();

    chatMessageRepository.saveAndFlush(chatMessage);

    chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());

    participant.updateLastReadMessage(chatMessage);

    return ChatMessageResponse.from(chatMessage);
  }
}
