package com.example.WonkaoTalk.domain.auth.dto.OAuth;

public interface OAuth2UserInfo {

  String getProvider();

  String getProviderId();

  String getEmail();

  String getName();

  String getPhone();
}
