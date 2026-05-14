package com.example.WonkaoTalk.domain.order.dto;

public record OrderCreateResponse(
    OrderCreateInfoDto orderInfo,
    PaymentCreateInfoDto paymentInfo
) {

  public record OrderCreateInfoDto(
      Long orderId,
      String orderNumber,
      String orderTitle,
      Long originalAmount,
      Long discountAmount,
      Long pointUsedAmount,
      Long finalAmount
  ) {

  }

  // Toss에 요청할때 사용할 정보 따로 뻄. (front에서 처리하기 쉽게하려고..)
  public record PaymentCreateInfoDto(
      Long paymentId,
      String tossOrderId, // Memo: pgOrderId로 할까 고민중...
      Long amount,
      String orderName
  ) {

  }
}