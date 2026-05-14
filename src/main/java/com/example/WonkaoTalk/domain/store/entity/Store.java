package com.example.WonkaoTalk.domain.store.entity;

import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.store.enums.StoreStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE stores SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "stores")
public class Store {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private String phone;

  @Builder.Default
  @Column(nullable = false)
  private String thumbnail = "http://defaultThumbnail.png";

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StoreStatus status = StoreStatus.ACTIVE;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "seller_id", nullable = false, unique = true) // 현재 스토어와 판매자는 1대1 연관
  private Seller seller;

  @CreatedDate()
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void updateInfo(String name, String description, String phone, String thumbnail) {
    this.name = name;
    this.description = description;
    this.phone = phone;
    this.thumbnail = thumbnail;
  }

  public void deleteStore() {
    this.name = "삭제된 스토어" + UUID.randomUUID().toString().substring(8);
    this.description = "DELETED";
    this.phone = "000-0000-0000";
    this.thumbnail = "http://defaultThumbnail.png";
    this.status = StoreStatus.DELETED;
    this.deletedAt = LocalDateTime.now();
  }
}
