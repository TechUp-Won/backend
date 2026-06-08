package com.example.WonkaoTalk.domain.order.entity;

public enum OrderStatus {
  CREATED,            // 주문 생성됨, 아직 결제 시도 전 또는 결제 준비 상태
  PAYMENT_PENDING,    // PG 결제창 진행 중
  PAID,               // 결제 완료
  CANCELED,           // 결제 도중 취소 (결제 미진행)
  EXPIRED,            // 주문 만료
  REFUNDED,             // 환불
  COMPLETED           // 구매 확정 또는 주문 완료
}
