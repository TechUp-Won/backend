package com.example.WonkaoTalk.domain.payment.entity;

import com.example.WonkaoTalk.domain.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_id")
  private Long paymentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(name = "pg_provider", nullable = false)
  private PgProvider pgProvider;

  @Column(name = "toss_order_id", nullable = false, unique = true)
  private String tossOrderId;

  @Column(name = "payment_key", unique = true)
  private String paymentKey;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;

  @Column(name = "total_amount", nullable = false)
  private Long totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PaymentStatus status;

  @Column(name = "fail_code")
  private String failCode;

  @Column(name = "fail_message")
  private String failMessage;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "failed_at")
  private LocalDateTime failedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private Payment(Order order, PgProvider pgProvider, String tossOrderId, String idempotencyKey,
      Long totalAmount, PaymentStatus status, LocalDateTime requestedAt) {
    this.order = order;
    this.pgProvider = pgProvider;
    this.tossOrderId = tossOrderId;
    this.idempotencyKey = idempotencyKey;
    this.totalAmount = totalAmount;
    this.status = status;
    this.requestedAt = requestedAt;
  }

  public static Payment createReadyPayment(
      Order order,
      String tossOrderId,
      String idempotencyKey,
      Long totalAmount
  ) {
    return new Payment(
        order,
        PgProvider.TOSS_PAYMENTS,
        tossOrderId,
        idempotencyKey,
        totalAmount,
        PaymentStatus.READY,
        LocalDateTime.now()
    );
  }

  public void markPending() {
    this.status = PaymentStatus.PENDING;
  }

  public void markPaid(String paymentKey) {
    this.paymentKey = paymentKey;
    this.status = PaymentStatus.PAID;
    this.approvedAt = LocalDateTime.now();
  }

  public void markFailed(String failCode, String failMessage) {
    this.failCode = failCode;
    this.failMessage = failMessage;
    this.status = PaymentStatus.FAILED;
    this.failedAt = LocalDateTime.now();
  }

  public void markCanceled(String failCode, String failMessage) {
    this.failCode = failCode;
    this.failMessage = failMessage;
    this.status = PaymentStatus.CANCELED;
    this.failedAt = LocalDateTime.now();
  }

  public void markInvalid(String failCode, String failMessage) {
    this.failCode = failCode;
    this.failMessage = failMessage;
    this.status = PaymentStatus.INVALID;
    this.failedAt = LocalDateTime.now();
  }
}
