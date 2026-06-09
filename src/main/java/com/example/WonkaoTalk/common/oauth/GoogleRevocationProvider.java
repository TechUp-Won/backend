package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GoogleRevocationProvider implements OAuthRevocationProvider {

  private final RestClient restClient;

  public GoogleRevocationProvider() {
    this.restClient = RestClient.create("https://oauth2.googleapis.com");
  }

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
        .uri("/revoke?token=" + providerAccessToken)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .retrieve()
        .toBodilessEntity();
  }
}
