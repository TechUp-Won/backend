package com.example.WonkaoTalk.domain.auth.dto;

public interface OAuth2UserInfo {

  String getProvider();

  String getProviderId();

  String getEmail();

  String getName();
}
