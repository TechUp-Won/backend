package com.example.WonkaoTalk.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderCreateResponseDto {

  private Long orderId;

  private String orderNumber;

  private String orderTitle;

  private Integer originalAmount;

  private Integer discountAmount;

  private Integer finalAmount;
}
