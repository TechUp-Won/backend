package com.example.WonkaoTalk.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "product")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "product_id")
  private Long productId;

  @Column(name = "store_id", nullable = false)
  // TODO: Store Entity가 만들어 지면 타입 변경 필요
  private Long storeId;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "thumbnail")
  private String thumbnail;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(name = "discount_rate")
  private Integer discountRate;

  @Column(name = "price", nullable = false)
  private Integer price;

  @Formula("price * (100 - COALESCE(discount_rate, 0)) / 100") // price와 discount_rate에서 계산되는 파생값
  private Integer discountedPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SaleStatus status;

  @Column(name = "like_count", nullable = false)
  private Integer likeCount = 0;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
