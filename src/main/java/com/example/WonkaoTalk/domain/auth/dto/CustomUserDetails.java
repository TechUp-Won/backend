package com.example.WonkaoTalk.domain.auth.dto;

import java.util.Collection;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class CustomUserDetails extends User {

  private final Long authId;

  @Builder
  public CustomUserDetails(
      String email, String password, Long authId,
      Collection<? extends GrantedAuthority> authorities) {
    super(email, password != null ? password : "", authorities);
    this.authId = authId;
  }

}
