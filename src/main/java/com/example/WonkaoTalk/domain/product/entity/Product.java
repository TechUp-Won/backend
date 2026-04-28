package com.example.WonkaoTalk.domain.product.entity;

import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
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
import lombok.Getter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "products")
@Getter
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "store_id", nullable = false)
  // TODO: Store 엔티티 구현 시 @ManyToOne 관계로 변경 및 연관관계 매핑 필요
  private Long storeId;

  @Column(name = "name", nullable = false)
  private String name;

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
