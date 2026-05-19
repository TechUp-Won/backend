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
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

  private final ChatMessageRepo chatMessageRepo;
  private final ChatRoomRepo chatRoomRepo;
  private final ChatParticipantRepo chatParticipantRepo;
  private final UserRepo userRepo;

  @Transactional
  public ChatMessageResponse sendMessage(Long userId, Long chatRoomId, ChatMessageRequest request) {
    ChatRoom chatRoom = chatRoomRepo.findById(chatRoomId)
        .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

    ChatParticipant participant = chatParticipantRepo.findByChatRoomIdAndUserId(chatRoomId,
            userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

    User me = userRepo.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    ChatMessage answerMessage = null;
    if (request.answerMessageId() != null) {
      answerMessage = chatMessageRepo.findById(request.answerMessageId())
          .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
    }

    ChatMessage chatMessage = ChatMessage.builder()
        .chatRoom(chatRoom)
        .senderId(userId)
        .content(request.content())
        .messageType(request.messageType())
        .answerMessage(answerMessage)
        .build();

    chatMessageRepo.saveAndFlush(chatMessage);
    chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedAt());
    participant.updateLastReadMessage(chatMessage);

    List<Long> allReadMessageIds = chatParticipantRepo.findAllParticipantsLastReadMessageIds(
        chatRoomId);
    int unreadCount = calculateUnreadCount(chatMessage.getId(), allReadMessageIds);

    return ChatMessageResponse.of(chatMessage, me.getNickname(), unreadCount);
  }

  @Transactional
  public ChatMessageListResponse getMessageList(Long userId, Long chatRoomId, Long cursorId,
      int size) {
    ChatParticipant myParticipant = chatParticipantRepo.findByChatRoomIdAndUserId(chatRoomId,
            userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

    PageRequest pageRequest = PageRequest.of(0, size);
    Slice<ChatMessage> messageSlice = chatMessageRepo.findMessagesByCursor(chatRoomId,
        cursorId, pageRequest);

    List<Long> allReadMessageIds = chatParticipantRepo.findAllParticipantsLastReadMessageIds(
        chatRoomId);

    Set<Long> senderIds = messageSlice.getContent().stream()
        .map(ChatMessage::getSenderId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Map<Long, String> senderNicknameMap = userRepo.findAllById(senderIds).stream()
        .collect(Collectors.toMap(User::getId, User::getNickname));

    List<ChatMessageDto> messageDtoList = messageSlice.getContent().stream()
        .map(message -> {
          int unreadCount = calculateUnreadCount(message.getId(), allReadMessageIds);

          String senderNickname = senderNicknameMap.getOrDefault(message.getSenderId(), "(알 수 없음)");

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

  private int calculateUnreadCount(Long currentMessageId, List<Long> allReadMessageIds) {
    int unreadCount = 0;
    for (Long readId : allReadMessageIds) {
      if (readId == null || currentMessageId > readId) {
        unreadCount++;
      }
    }
    return unreadCount;
  }
}
