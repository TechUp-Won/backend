package com.example.WonkaoTalk.domain.auth.entity;

import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "login_history")
public class LoginHistory {

  @CreatedDate
  @Column(name = "login_at", nullable = false)
  LocalDateTime loginAt;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "auth_id", nullable = false)
  private Long authId;
  @Column(name = "ip_address")
  private String ipAddress;
  @Column(name = "user_agent")
  private String userAgent;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LoginStatus status;

}
