package com.example.WonkaoTalk.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailRequest(
    String orderId,
    @NotBlank String code,
    @NotBlank String message
) {

}
