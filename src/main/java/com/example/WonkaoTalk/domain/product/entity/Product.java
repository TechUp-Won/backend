package com.example.WonkaoTalk.domain.product.entity;

import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
import com.example.WonkaoTalk.domain.store.entity.Store;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "products")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

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

  @Column(name = "discounted_price", nullable = false)
  private Integer discountedPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SaleStatus status;

  @Builder.Default
  @Column(name = "like_count", nullable = false)
  private Integer likeCount = 0;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void update(String name, Category category, String thumbnail,
      Integer price, Integer discountRate, SaleStatus status) {
    if (name != null) this.name = name;
    if (category != null) this.category = category;
    if (thumbnail != null) this.thumbnail = thumbnail;

    boolean recalcPrice = price != null || discountRate != null;
    if (price != null) this.price = price;
    if (discountRate != null) this.discountRate = discountRate;
    if (recalcPrice) {
      int dr = this.discountRate != null ? this.discountRate : 0;
      this.discountedPrice = (int) Math.round(this.price * (1 - dr / 100.0));
    }

    if (status != null) this.status = status;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.status = SaleStatus.STOP_SALE;
  }

  public boolean isOnSale() {
    return this.deletedAt == null && this.status == SaleStatus.ON_SALE;
  }
}
