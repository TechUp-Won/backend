package com.example.WonkaoTalk.domain.payment.client;

public record TossPaymentConfirmResult(
    String paymentKey,
    String orderId,
    String status,
    String method,
    Long totalAmount,
    String approvedAt,
    Receipt receipt
) {

  public record Receipt(String url) {

  }

  public String receiptUrl() {
    return receipt == null ? null : receipt.url();
  }
}
