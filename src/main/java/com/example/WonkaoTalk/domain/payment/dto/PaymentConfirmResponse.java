package com.example.WonkaoTalk.domain.payment.dto;

public record PaymentConfirmResponse(
    Long paymentId,
    Long orderId,
    String tossOrderId,
    String paymentKey,
    Long amount,
    String status,
    String method,
    String approvedAt,
    String receiptUrl
) {

}
