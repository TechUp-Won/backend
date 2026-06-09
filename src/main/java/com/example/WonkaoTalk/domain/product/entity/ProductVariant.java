package com.example.WonkaoTalk.domain.product.entity;

import com.example.WonkaoTalk.domain.product.enums.SaleStatus;
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
@Table(name = "product_variants")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductVariant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "stock", nullable = false)
  private Integer stock;

  @Column(name = "name")
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SaleStatus status;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void adjustStock(int changeAmount) {
    if (this.stock + changeAmount < 0) {
      throw new IllegalArgumentException("재고는 0 미만이 될 수 없습니다.");
    }
    this.stock += changeAmount;
    if (this.stock == 0 && this.status == SaleStatus.ON_SALE) {
      this.status = SaleStatus.OUT_OF_STOCK;
    } else if (this.stock > 0 && this.status == SaleStatus.OUT_OF_STOCK
        && this.product.getStatus() != SaleStatus.STOP_SALE) {
      this.status = SaleStatus.ON_SALE;
    }
  }

  public boolean isSellable() {
    return this.deletedAt == null && this.product.isOnSale() && this.status == SaleStatus.ON_SALE;
  }
}
