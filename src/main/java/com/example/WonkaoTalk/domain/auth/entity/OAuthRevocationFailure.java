package com.example.WonkaoTalk.domain.auth.entity;

import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import com.example.WonkaoTalk.domain.auth.enums.FallbackStatus;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "oauth_revocation_failures")
public class OAuthRevocationFailure {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider;

  @Column(nullable = false)
  private String providerUserId;

  @Column(length = 500)
  private String providerRefreshToken;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FallbackStatus status = FallbackStatus.PENDING;

  @Column(nullable = false)
  private int retryCount;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  @Builder
  public OAuthRevocationFailure(AuthProvider provider, String providerUserId,
      String providerRefreshToken) {
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.providerRefreshToken = providerRefreshToken;
    this.status = FallbackStatus.PENDING;
  }

  public void markAsSuccess() {
    this.status = FallbackStatus.SUCCESS;
  }

  public void incrementRetryCount(int maxRetry) {
    this.retryCount++;
    if (this.retryCount >= maxRetry) {
      this.status = FallbackStatus.PERMANENT_FAILURE;
    }
  }
}