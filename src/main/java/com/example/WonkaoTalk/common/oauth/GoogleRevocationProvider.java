package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GoogleRevocationProvider implements OAuthRevocationProvider {

  private final RestClient restClient;

  @Override
  public boolean supports(AuthProvider provider) {
    return provider == AuthProvider.GOOGLE;
  }

  @Override
  public void revoke(String providerId, String providerAccessToken) {
    if (!StringUtils.hasText(providerAccessToken)) {
      throw new BusinessException(ErrorCode.OAUTH_NULL_TOKEN);
    }

    restClient.post()
        .uri("https://oauth2.googleapis.com/revoke?token=" + providerAccessToken)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .retrieve()
        .toBodilessEntity();
  }
}
