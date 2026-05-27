package com.example.WonkaoTalk.domain.order.entity;

import com.example.WonkaoTalk.domain.product.entity.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant productVariant;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "option_summary")
  private String optionSummary;

  @Column(name = "product_amount", nullable = false)
  private Long productAmount;

  @Column
  private Integer quantity;

  @Column(name = "product_image_url", columnDefinition = "TEXT")
  private String productImageUrl;

  private OrderItem(
      Order order,
      ProductVariant productVariant,
      String productName,
      String optionSummary,
      Long productAmount,
      Integer quantity,
      String productImageUrl
  ) {
    this.order = order;
    this.productVariant = productVariant;
    this.productName = productName;
    this.optionSummary = optionSummary;
    this.productAmount = productAmount;
    this.quantity = quantity;
    this.productImageUrl = productImageUrl;
  }

  public static OrderItem createOrderItem(
      Order order,
      ProductVariant productVariant,
      String productName,
      String optionSummary,
      Long productAmount,
      Integer quantity,
      String productImageUrl
  ) {
    return new OrderItem(
        order,
        productVariant,
        productName,
        optionSummary,
        productAmount,
        quantity,
        productImageUrl
    );
  }
}
