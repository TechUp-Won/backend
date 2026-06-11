package com.example.WonkaoTalk.domain.user.entity;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.user.enums.Gender;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String nickname;

  @Builder.Default
  @Column(nullable = false)
  private String image = "http://defaultImage.png"; // TODO: 기본 프로필 이미지 URL 작성 필요

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(nullable = false)
  private String name;

  @Column(length = 13, unique = true)
  private String phone;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender = Gender.NONE;

  @Builder.Default
  @Column(name = "marketing_agree", nullable = false)
  private boolean marketingAgree = false;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "auth_id", nullable = false)
  private Auth auth;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void withdraw() {
    this.nickname = "탈퇴한사용자" + UUID.randomUUID().toString().substring(0, 8);
    this.image = "http://defaultImage.png";
    this.birthDate = null;
    this.name = "Unknown";
    this.phone = "000-0000-" + UUID.randomUUID().toString().substring(0, 4);
    this.gender = Gender.NONE;
    this.marketingAgree = false;
    this.deletedAt = LocalDateTime.now();
  }

  public void update(String nickname, String image, Gender gender, LocalDate birthDate,
      boolean marketingAgree) {
    this.nickname = nickname;
    this.image = image;
    this.gender = gender;
    this.birthDate = birthDate;
    this.marketingAgree = marketingAgree;
  }
}
