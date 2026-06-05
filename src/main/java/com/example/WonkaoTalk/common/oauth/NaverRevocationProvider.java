package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class NaverRevocationProvider implements OAuthRevocationProvider {

  private final RestClient restClient;
  @Value("${spring.security.oauth2.client.registration.naver.client-id}")
  private String clientId;
  @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
  private String clientSecret;

  @Override
  public boolean supports(AuthProvider provider) {
    return provider == AuthProvider.NAVER;
  }

  @Override
  public void revoke(String providerId, String providerAccessToken) {
    if (!StringUtils.hasText(providerAccessToken)) {
      throw new BusinessException(ErrorCode.OAUTH_NULL_TOKEN);
    }

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "delete");
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("access_token", providerAccessToken);
    formData.add("service_provider", "NAVER");

    restClient.post()
        .uri("https://nid.naver.com/oauth2.0/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(formData)
        .retrieve()
        .toBodilessEntity();
  }
}
