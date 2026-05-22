package com.example.WonkaoTalk.domain.product.entity;

import com.example.WonkaoTalk.domain.order.entity.Order;
import com.example.WonkaoTalk.domain.product.enums.StockChangeReason;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "stock_histories")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StockHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_variant_id", nullable = false)
  private ProductVariant productVariant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private Order order;

  @Column(name = "change_amount", nullable = false)
  private Integer changeAmount;

  @Column(name = "stock_before", nullable = false)
  private Integer stockBefore;

  @Column(name = "stock_after", nullable = false)
  private Integer stockAfter;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, length = 20)
  private StockChangeReason reason;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public static StockHistory of(
      ProductVariant productVariant,
      Order order,
      int changeAmount,
      int stockBefore,
      StockChangeReason reason
  ) {
    return StockHistory.builder()
        .productVariant(productVariant)
        .order(order)
        .changeAmount(changeAmount)
        .stockBefore(stockBefore)
        .stockAfter(stockBefore + changeAmount)
        .reason(reason)
        .build();
  }
}
