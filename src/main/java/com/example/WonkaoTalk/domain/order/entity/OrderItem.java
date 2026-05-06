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

@Entity
@Table(name = "order_items")
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
  private Integer productAmount;

  @Column
  private Integer quantity;
}
