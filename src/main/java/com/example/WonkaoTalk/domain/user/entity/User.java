package com.example.WonkaoTalk.domain.user.entity;

import com.example.WonkaoTalk.domain.user.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @Column(nullable = false, length = 20)
  private String nickname;

  @Column(nullable = false)
  private String image;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, length = 13)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender = Gender.NONE;

  @Column(name = "marketing_agree", nullable = false)
  private boolean marketingAgree = false;

  @Column(name = "auth_id")
  private Long authId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

}
