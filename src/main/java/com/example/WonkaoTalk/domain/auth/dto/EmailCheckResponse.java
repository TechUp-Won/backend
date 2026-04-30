package com.example.WonkaoTalk.domain.auth.dto;

import lombok.Builder;

@Builder
public record EmailCheckResponse(
    boolean isValid
) {

  public static EmailCheckResponse from(boolean isValid) {
    return EmailCheckResponse.builder()
        .isValid(isValid)
        .build();
  }
}
