package com.example.WonkaoTalk.domain.order.dto;

public record OrderCreateResponseDto(
    Long orderId,
    String orderNumber,
    String orderTitle,
    Long originalAmount,
    Long discountAmount,
    Long finalAmount
) {
}
