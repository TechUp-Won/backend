package com.example.WonkaoTalk.domain.payment.entity;

public enum PaymentStatus {
  READY,
  PENDING,
  PAID,
  ABORTED,      // 사용자가 결제창에서 결제 진행을 중단함
  FAILED,       // 카드 거절, 승인 실패, 인증 실패 등 결제 시도 실패
  CANCELED,     // 이미 PAID 된 결제를 취소 또는 환불함
  INVALID
}