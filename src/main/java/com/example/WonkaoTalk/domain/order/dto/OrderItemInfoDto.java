package com.example.WonkaoTalk.domain.order.dto;

import com.example.WonkaoTalk.domain.order.entity.OrderItem;

public record OrderItemInfoDto(
    String productName,
    String optionSummary,
    Long productAmount,
    int quantity,
    String productImageUrl
) {

  public static OrderItemInfoDto from(OrderItem orderItem) {
    return new OrderItemInfoDto(
        orderItem.getProductName(),
        orderItem.getOptionSummary(),
        orderItem.getProductAmount(),
        orderItem.getQuantity(),
        orderItem.getProductImageUrl()
    );
  }

}
