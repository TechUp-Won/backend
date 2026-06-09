package com.example.WonkaoTalk.domain.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tosspayments")
public class TossPaymentsProperties {

  private String clientKey = "";
  private String secretKey = "";
  private String baseUrl = "https://api.tosspayments.com";
  private String successUrl = "";
  private String failUrl = "";
}
