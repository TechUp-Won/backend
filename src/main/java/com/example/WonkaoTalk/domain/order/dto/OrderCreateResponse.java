package com.example.WonkaoTalk.domain.order.dto;

public record OrderCreateResponse(
    Long orderId,
    String orderNumber,
    String orderTitle,
    Long originalAmount,
    Long discountAmount,
    Long finalAmount
) {
}
