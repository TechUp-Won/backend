package com.example.WonkaoTalk.domain.chat.entity;

import com.example.WonkaoTalk.domain.chat.enums.RoomStatus;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_rooms")
public class ChatRoom {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoomType roomType;

  private String title;

  @Column(columnDefinition = "TEXT")
  private String roomImage;

  @Builder.Default
  @Column(nullable = false)
  private int participantCount = 0;

  @Column(columnDefinition = "TEXT")
  private String lastMessageContent;

  private LocalDateTime lastMessageAt;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(nullable = false, length = 20)
  private RoomStatus roomStatus = RoomStatus.ACTIVE;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column
  private LocalDateTime deletedAt;
}
