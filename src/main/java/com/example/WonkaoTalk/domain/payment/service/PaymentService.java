package com.example.WonkaoTalk.domain.payment.service;

import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.payment.entity.Payment;
import com.example.WonkaoTalk.domain.payment.repo.PaymentRepo;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepo paymentRepo;

  // 주문 생성
  public Payment createReadyPayment(Order order) {
    // 토스 주문 번호 만들기
    String tossOrderId = generateTossOrderId(order.getOrderNumber());

    // 멱등키 만들기
    String idempotencyKey = generateIdempotencyKey();

    LocalDateTime requestAt = LocalDateTime.now();

    Payment payment = Payment.createReadyPayment(
        order,
        tossOrderId,
        idempotencyKey,
        order.getFinalAmount(),
        requestAt
    );

    return paymentRepo.save(payment);

  }

  private String generateTossOrderId(String orderNumber) {
    return orderNumber + "-PAY-" + UUID.randomUUID();
  }

  private String generateIdempotencyKey() {
    return UUID.randomUUID().toString();
  }
}
