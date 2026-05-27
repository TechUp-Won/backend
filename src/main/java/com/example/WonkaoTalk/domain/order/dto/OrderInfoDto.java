package com.example.WonkaoTalk.domain.order.dto;

import com.example.WonkaoTalk.domain.order.entity.Order;

public record OrderInfoDto(
    Long orderId,
    String orderNumber,
    String orderTitle,
    Long originalAmount,
    Long discountAmount,
    Long pointUsedAmount,
    Long finalAmount
) {

  public static OrderInfoDto from(Order order) {
    return new OrderInfoDto(
        order.getOrderId(),
        order.getOrderNumber(),
        order.getOrderTitle(),
        order.getOriginalAmount(),
        order.getDiscountAmount(),
        order.getPointUsedAmount(),
        order.getFinalAmount()
    );
  }
}
