package com.example.WonkaoTalk.domain.order.entity;

public enum OrderStatus {
  CREATED,            // 주문 생성됨, 아직 결제 시도 전 또는 결제 준비 상태
  PAYMENT_PENDING,    // PG 결제창 진행 중
  PAID,               // 결제 완료
  PAYMENT_FAILED,     // 결제 실패
  PAYMENT_CANCELED,   // 사용자가 결제창에서 취소
  CANCELED,           // 결제 완료 후 주문 취소
  COMPLETED           // 구매 확정 또는 주문 완료
}
