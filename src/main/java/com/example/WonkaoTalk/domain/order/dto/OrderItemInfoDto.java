package com.example.WonkaoTalk.domain.order.dto;

import com.example.WonkaoTalk.domain.order.entity.OrderItem;

public record OrderItemInfoDto(
    String productName,
    String optionSummary,
    Long productAmount,
    int quantity
    // TODO: 썸네일도 추가해야함. OrderItem에 썸네일 스냅샷 추가 후 변경 예정
) {

  public static OrderItemInfoDto from(OrderItem orderItem) {
    return new OrderItemInfoDto(
        orderItem.getProductName(),
        orderItem.getOptionSummary(),
        orderItem.getProductAmount(),
        orderItem.getQuantity()
    );
  }

}
