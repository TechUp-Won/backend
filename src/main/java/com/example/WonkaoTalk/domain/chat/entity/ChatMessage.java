package com.example.WonkaoTalk.domain.chat.entity;

import com.example.WonkaoTalk.domain.chat.enums.MessageStatus;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_messages")
public class ChatMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  private Long senderId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "answer_message_id")
  private ChatMessage answerMessage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MessageType messageType;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Builder.Default
  private int likeCount = 0;

  @Version
  private Long version;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private MessageStatus messageStatus = MessageStatus.NORMAL;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime deletedAt;
}
