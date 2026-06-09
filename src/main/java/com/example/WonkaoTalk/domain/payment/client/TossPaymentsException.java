package com.example.WonkaoTalk.domain.payment.client;

import lombok.Getter;

@Getter
public class TossPaymentsException extends RuntimeException {

  private final String code;

  public TossPaymentsException(String code, String message) {
    super(message);
    this.code = code;
  }
}
