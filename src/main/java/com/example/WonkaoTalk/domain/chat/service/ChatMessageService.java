package com.example.WonkaoTalk.domain.chat.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageListResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageListResponse.ChatMessageDto;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;
import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepository;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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

  @Transactional
  public ChatMessageListResponse getMessageList(Long userId, Long chatRoomId, Long cursorId,
      int size) {
    ChatParticipant myParticipant = chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId,
            userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

    PageRequest pageRequest = PageRequest.of(0, size);
    Slice<ChatMessage> messageSlice = chatMessageRepository.findMessagesByCursor(chatRoomId,
        cursorId, pageRequest);

    List<Long> otherReadMessageIds = chatParticipantRepository.findOtherParticipantsLastReadMessageIds(
        chatRoomId, userId);

    List<ChatMessageDto> messageDtoList = messageSlice.getContent().stream()
        .map(message -> {
          int unreadCount = calculateUnreadCount(message.getId(), otherReadMessageIds);

          // TODO 연동 후 실제 닉네임
          String senderNickname =
              (message.getSenderId() != null && message.getSenderId().equals(userId)) ? "나" : "상대방";

          return ChatMessageDto.of(message, userId, senderNickname, unreadCount);
        }).toList();

    if (!messageSlice.getContent().isEmpty()) {
      ChatMessage latestMessageFetched = messageSlice.getContent().getFirst();

      if (myParticipant.getLastReadMessage() == null ||
          myParticipant.getLastReadMessage().getId() < latestMessageFetched.getId()) {
        myParticipant.updateLastReadMessage(latestMessageFetched);
      }
    }

    Long nextCursorId = null;
    if (messageSlice.hasNext() && !messageSlice.getContent().isEmpty()) {
      nextCursorId = messageSlice.getContent().getLast().getId();
    }

    return ChatMessageListResponse.of(messageDtoList, messageSlice.hasNext(), nextCursorId);
  }

  private int calculateUnreadCount(Long currentMessageId, List<Long> otherReadMessageIds) {
    int unreadCount = 0;
    for (Long readId : otherReadMessageIds) {
      if (readId == null || currentMessageId > readId) {
        unreadCount++;
      }
    }
    return unreadCount;
  }
}
