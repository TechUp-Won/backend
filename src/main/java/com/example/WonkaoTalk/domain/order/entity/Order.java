package com.example.WonkaoTalk.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "order_number", nullable = false)
  private String orderNumber;

  @Column(name = "user_id")
  // TODO: User Entity가 만들어 지면 타입 변경 필요
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status", nullable = false)
  private  OrderStatus orderStatus;

  @Column(name = "original_amount")
  private Integer originalAmount;

  @Column(name = "discount_amount")
  private Integer discountAmount;

  @Column(name = "final_amount", nullable = false)
  private Integer finalAmount;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "title")
  // 주문 명 (xx외 2건)
  private String orderTitle;
}
