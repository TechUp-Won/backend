package com.example.WonkaoTalk.domain.auth.dto;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

  private final Auth auth;
  private final Map<String, Object> attributes;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + auth.getRole().name()));
  }

  @Override
  public String getName() {
    return auth.getId().toString();
  }
}
