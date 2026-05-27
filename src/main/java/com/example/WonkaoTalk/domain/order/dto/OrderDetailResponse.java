package com.example.WonkaoTalk.domain.order.dto;

import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.entity.PaymentStatus;
import com.example.WonkaoTalk.domain.payment.entity.PgProvider;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
    // 주문정보
    OrderInfoDto orderInfo,
    // 상품정보
    List<OrderItemInfoDto> orderItemInfoList,
    // 결제정보
    List<PaymentInfoDto> paymentInfo

) {

  public record PaymentInfoDto(
      Long paymentId,
      PaymentStatus status,
      PgProvider pgProvider,
      Long totalAmount,
      LocalDateTime requestedAt,
      LocalDateTime approvedAt
  ) {

    public static PaymentInfoDto from(Payment payment) {
      return new PaymentInfoDto(
          payment.getPaymentId(),
          payment.getStatus(),
          payment.getPgProvider(),
          payment.getTotalAmount(),
          payment.getRequestedAt(),
          payment.getApprovedAt()
      );
    }

  }

}