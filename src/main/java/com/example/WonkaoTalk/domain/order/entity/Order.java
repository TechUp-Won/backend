package com.example.WonkaoTalk.domain.order.entity;

import com.example.WonkaoTalk.domain.user.entity.User;
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
@EntityListeners(AuditingEntityListener.class)
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "order_number", nullable = false)
  private String orderNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status", nullable = false)
  private OrderStatus orderStatus;

  @Column(name = "original_amount")
  private Long originalAmount;

  @Column(name = "discount_amount")
  private Long discountAmount;

  @Column(name = "point_used_amount", nullable = false)
  private Long pointUsedAmount;

  @Column(name = "final_amount", nullable = false)
  private Long finalAmount;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "title")
  // 주문 명 (xx외 2건)
  private String orderTitle;

  public static Order createOrder(
      String orderNumber,
      User user,
      Long originalAmount,
      Long discountAmount,
      Long pointUsedAmount,
      Long finalAmount,
      String orderTitle
  ) {
    Order order = new Order();
    order.orderNumber = orderNumber;
    order.user = user;
    order.orderStatus = OrderStatus.CREATED;
    order.originalAmount = originalAmount;
    order.discountAmount = discountAmount;
    order.pointUsedAmount = pointUsedAmount;
    order.finalAmount = finalAmount;
    order.orderTitle = orderTitle;
    return order;
  }

  public void markPaymentPending() {
    this.orderStatus = OrderStatus.PAYMENT_PENDING;
  }

  public void markPaid() {
    this.orderStatus = OrderStatus.PAID;
  }

  public void markPaymentFailed() {
    this.orderStatus = OrderStatus.PAYMENT_FAILED;
  }

  public void markPaymentCanceled() {
    this.orderStatus = OrderStatus.PAYMENT_CANCELED;
  }
}
