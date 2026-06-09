package com.example.WonkaoTalk.domain.auth.dto;

import java.util.Collection;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class CustomUserDetails extends User {

  private final Long authId;
  private final Long userId;
  private final Long sellerId;

  @Builder(builderMethodName = "customBuilder")
  public CustomUserDetails(
      String email, String password, Long authId, Long userId, Long sellerId,
      Collection<? extends GrantedAuthority> authorities) {
    super(email, password != null ? password : "", authorities);
    this.authId = authId;
    this.userId = userId;
    this.sellerId = sellerId;
  }
}
