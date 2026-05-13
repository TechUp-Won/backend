package com.example.WonkaoTalk.domain.shipping.entity;

import com.example.WonkaoTalk.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "shipping_address")
public class ShippingAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "shipping_address_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "recipient_name", nullable = false)
  private String recipientName;

  @Column(name = "recipient_phone", nullable = false)
  private String recipientPhone;

  @Column(name = "zip_code", nullable = false)
  private String zipCode;

  @Column(name = "address1", nullable = false)
  private String address1;

  @Column(name = "address2")
  private String address2;

  @Builder.Default
  @Column(name = "is_default", nullable = false)
  private boolean isDefault = false;

  @Column(name = "memo")
  private String memo;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public void update(String recipientName, String recipientPhone, String zipCode,
      String address1, String address2, String memo) {
    if (recipientName != null) this.recipientName = recipientName;
    if (recipientPhone != null) this.recipientPhone = recipientPhone;
    if (zipCode != null) this.zipCode = zipCode;
    if (address1 != null) this.address1 = address1;
    this.address2 = address2;
    this.memo = memo;
  }

  public void setAsDefault() {
    this.isDefault = true;
  }

  public void unsetDefault() {
    this.isDefault = false;
  }
}
