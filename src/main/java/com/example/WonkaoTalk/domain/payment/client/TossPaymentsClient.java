package com.example.WonkaoTalk.domain.payment.client;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.payment.config.TossPaymentsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TossPaymentsClient {

  private final TossPaymentsProperties properties;
  private final ObjectMapper objectMapper;

  public TossPaymentConfirmResult confirm(String paymentKey, String orderId, Long amount,
      String idempotencyKey) {
    if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
      throw new BusinessException(ErrorCode.PAYMENT_SECRET_KEY_NOT_CONFIGURED);
    }

    try {
      return RestClient.create(properties.getBaseUrl())
          .post()
          .uri("/v1/payments/confirm")
          .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
          .header("Idempotency-Key", idempotencyKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of(
              "paymentKey", paymentKey,
              "orderId", orderId,
              "amount", amount
          ))
          .retrieve()
          .body(TossPaymentConfirmResult.class);
    } catch (RestClientResponseException e) {
      throw toTossException(e);
    }
  }

  private String authorizationHeader() {
    String token = Base64.getEncoder()
        .encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));
    return "Basic " + token;
  }

  private TossPaymentsException toTossException(RestClientResponseException e) {
    try {
      JsonNode root = objectMapper.readTree(e.getResponseBodyAsString());
      String code = root.path("code").asText("TOSS_PAYMENT_ERROR");
      String message = root.path("message").asText("토스페이먼츠 결제 승인에 실패했습니다.");
      return new TossPaymentsException(code, message);
    } catch (Exception ignored) {
      return new TossPaymentsException("TOSS_PAYMENT_ERROR", "토스페이먼츠 결제 승인에 실패했습니다.");
    }
  }
}
