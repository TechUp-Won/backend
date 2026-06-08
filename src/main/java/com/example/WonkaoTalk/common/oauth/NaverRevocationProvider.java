package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class NaverRevocationProvider implements OAuthRevocationProvider {

  private final String clientId;
  private final String clientSecret;

  public NaverRevocationProvider(
      @Value("${spring.security.oauth2.client.registration.naver.client-id}")
      String clientId,
      @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
      String clientSecret
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public boolean supports(AuthProvider provider) {
    return provider == AuthProvider.NAVER;
  }

  @Override
  public void revoke(String providerId, String providerRefreshToken) {
    if (!StringUtils.hasText(providerRefreshToken)) {
      throw new BusinessException(ErrorCode.OAUTH_NULL_TOKEN);
    }

    String newAccessToken = refreshNaverAccessToken(providerRefreshToken);
    if (newAccessToken == null) {
      // 상위 catch용 예외 발행
      throw new IllegalStateException("네이버 Access Token 갱신 실패로 해지 불가");
    }
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "delete");
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("access_token", newAccessToken);
    formData.add("service_provider", "NAVER");

    RestClient.create("https://nid.naver.com").post()
        .uri("/oauth2.0/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(formData)
        .retrieve()
        .toBodilessEntity();
  }

  private String refreshNaverAccessToken(String refreshToken) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "refresh_token");
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("refresh_token", refreshToken);

    Map<String, Object> response = RestClient.create("https://nid.naver.com").post()
        .uri("/oauth2.0/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(formData)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {
        });

    return response != null ? (String) response.get("access_token") : null;
  }
}
