package com.example.WonkaoTalk.domain.order.dto;

public record OrderCreateResponseDto(
    Long orderId,
    String orderNumber,
    String orderTitle,
    Integer originalAmount,
    Integer discountAmount,
    Integer finalAmount
) {
}
