package com.example.WonkaoTalk.domain.order.dto;

import com.example.WonkaoTalk.domain.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderListResponse(
    List<OrderSummaryDto> orders,
    PageInfoDto pageInfo
) {

  public record OrderSummaryDto(
      Long orderId,
      String orderNumber,
      String orderTitle,
      OrderStatus orderStatus,
      Long finalAmount,
      LocalDateTime createdAt
  ) {

  }
}
