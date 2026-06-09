package com.example.WonkaoTalk.domain.friend.entity;

import com.example.WonkaoTalk.domain.friend.enums.FriendStatus;
import com.example.WonkaoTalk.domain.user.entity.User;
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
import jakarta.persistence.UniqueConstraint;
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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "friends",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_user_friend",
            columnNames = {"user_id", "friend_id"}
        )})
public class Friend {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "friend_id", nullable = false)
  private User target;

  @Column(length = 20)
  private String alias;

  @Column(columnDefinition = "TEXT")
  private String memo;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FriendStatus status = FriendStatus.ACTIVE;

  @Builder.Default
  @Column(name = "is_favorite", nullable = false)
  private boolean isFavorite = false;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public void updateAlias(String newAlias) {
    this.alias = newAlias;
  }

  public void updateMemo(String newMemo) {
    this.memo = newMemo;
  }

  public void toggleFavorite() {
    this.isFavorite = !this.isFavorite;
  }

  public void changeStatus(FriendStatus newStatus) {
    this.status = newStatus;
  }
}
