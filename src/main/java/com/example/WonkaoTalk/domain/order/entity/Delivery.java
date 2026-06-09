package com.example.WonkaoTalk.domain.order.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deliveries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "recipient_name", nullable = false)
  private String recipientName;

  @Column(name = "recipient_phone", nullable = false)
  private String recipientPhone;

  @Column(nullable = false)
  private String zipcode;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false)
  private String addressDetail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus deliveryStatus;

  @Column
  private String memo;

  @Column
  private String deliveryCompany;

  @Column
  private String trackingNumber; // TODO: Long으로 할지 고민중.. 운송번호 보통 다 정수일것같은데 아닐수있어서..


  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  private Delivery(Order order, String recipientName, String recipientPhone, String zipcode,
      String address, String addressDetail, String memo, DeliveryStatus deliveryStatus) {
    this.order = order;
    this.recipientName = recipientName;
    this.recipientPhone = recipientPhone;
    this.zipcode = zipcode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.memo = memo;
    this.deliveryStatus = deliveryStatus;
  }

  public static Delivery createDelivery(Order order, String recipientName, String recipientPhone,
      String zipcode,
      String address, String addressDetail, String memo) {
    return new Delivery(order, recipientName, recipientPhone, zipcode, address, addressDetail, memo,
        DeliveryStatus.PREPARING);
  }

  public void markShipped(String deliveryCompany, String trackingNumber) {
    this.deliveryCompany = deliveryCompany;
    this.trackingNumber = trackingNumber;
    this.deliveryStatus = DeliveryStatus.SHIPPED;
  }

  public void markInTransit() {
    this.deliveryStatus = DeliveryStatus.IN_TRANSIT;
  }

  public void markDelivered() {
    this.deliveryStatus = DeliveryStatus.DELIVERED;
  }


}
