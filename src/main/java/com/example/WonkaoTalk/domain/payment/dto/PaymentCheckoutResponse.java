package com.example.WonkaoTalk.domain.payment.dto;

public record PaymentCheckoutResponse(
    Long paymentId,
    String clientKey,
    String tossOrderId,
    Long amount,
    String orderName,
    String successUrl,
    String failUrl
) {

}
