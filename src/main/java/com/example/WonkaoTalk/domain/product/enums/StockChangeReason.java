package com.example.WonkaoTalk.domain.product.enums;

public enum StockChangeReason {
  SALE,        // 결제 완료로 인한 차감
  CANCEL,      // 주문 취소로 인한 복구
  RESTOCK,     // 판매자 입고
  ADJUSTMENT   // 판매자 수동 조정
}
