package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;

public interface OAuthRevocationProvider {

  boolean supports(AuthProvider provider);

  void revoke(String providerId, String providerAccessToken);

}
