package com.example.WonkaoTalk.domain.payment.dto;

public record PaymentFailResponse(
    Long paymentId,
    String tossOrderId,
    String status,
    String failCode,
    String failMessage
) {

}
